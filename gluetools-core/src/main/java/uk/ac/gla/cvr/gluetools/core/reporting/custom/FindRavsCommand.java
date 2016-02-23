package uk.ac.gla.cvr.gluetools.core.reporting.custom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentAminoAcidFrequencyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentAminoAcidFrequencyResult;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamAminoAcidResult;


@CommandClass(
		commandWords={"find", "ravs"}, 
		description = "Find potential resistance associated variants in a SAM/BAM file", 
		docoptUsages = { "<fileName> <featureName>" },
		metaTags = {CmdMeta.consoleOnly}	
)
public class FindRavsCommand extends ModulePluginCommand<FindRavsResult, RavFinder> implements ProvidedProjectModeCommand {

	
	private String fileName;
	private String featureName;
	
	public FindRavsCommand() {
		super();
		
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
		this.featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
	}

	@Override
	protected FindRavsResult execute(CommandContext cmdContext, RavFinder ravFinder) {
		// S31Btch4-44_S44_L001_R1_001.val-Ref.3a.DE.x.HCVCENS1.X76918.fa.sam
		
		Pattern genotypePattern = Pattern.compile("Ref\\.(\\d)[a-z]+\\.");

		Matcher genotypeMatcher = genotypePattern.matcher(fileName);
		genotypeMatcher.find();
		String genotype = genotypeMatcher.group(1);
		String genotypeAlignmentName = "AL_" + genotype;
		
		ravFinder.log(Level.FINEST, "Genotype alignment: "+genotypeAlignmentName);
		
		ravFinder.log(Level.FINEST, "Finding amino acid frequencies for public sequences in "+genotypeAlignmentName);
		AlignmentAminoAcidFrequencyResult genotypeFrequencyResult;
		try(ModeCloser modeCloser = cmdContext.pushCommandMode("alignment", genotypeAlignmentName)) {
			genotypeFrequencyResult = cmdContext.cmdBuilder(AlignmentAminoAcidFrequencyCommand.class)
			.set(AlignmentAminoAcidFrequencyCommand.REFERENCE_NAME, "H77_AF009606")
			.set(AlignmentAminoAcidFrequencyCommand.RECURSIVE, true)
			.set(AlignmentAminoAcidFrequencyCommand.WHERE_CLAUSE, "sequence.source.name = 'ncbi-curated'")
			.set(AlignmentAminoAcidFrequencyCommand.FEATURE_NAME, featureName)
			.execute();
		}

		ravFinder.log(Level.FINEST, "Finding amino acids in file "+fileName);
		SamAminoAcidResult samAminoAcidResult;
		try(ModeCloser modeCloser = cmdContext.pushCommandMode("module", "samReporter")) {
			samAminoAcidResult = cmdContext.cmdBuilder(SamAminoAcidCommand.class)
			.set(SamAminoAcidCommand.FILE_NAME, fileName)
			.set(SamAminoAcidCommand.REFERENCE_NAME, "H77_AF009606")
			.set(SamAminoAcidCommand.FEATURE_NAME, featureName)
			.execute();
		}
		
		Map<VariantKey, Map<String,Object>> samFileVariants = new LinkedHashMap<VariantKey, Map<String, Object>>();
		for(Map<String,Object> samResultRow: samAminoAcidResult.asListOfMaps()) {
			int codon = (Integer) samResultRow.get(SamAminoAcidResult.CODON);
			String aa = (String) samResultRow.get(SamAminoAcidResult.AMINO_ACID);
			Double pctReads = (Double) samResultRow.get(SamAminoAcidResult.PERCENT_AA_READS);
			if(pctReads < ravFinder.getReadsMinPct()) {
				continue;
			}
			samFileVariants.put(new VariantKey(codon, aa), samResultRow);
		}

		Map<VariantKey, Map<String,Object>> genotypeVariants = new LinkedHashMap<VariantKey, Map<String, Object>>();
		for(Map<String,Object> genotypeResultRow: genotypeFrequencyResult.asListOfMaps()) {
			int codon = (Integer) genotypeResultRow.get(AlignmentAminoAcidFrequencyResult.CODON);
			String aa = (String) genotypeResultRow.get(AlignmentAminoAcidFrequencyResult.AMINO_ACID);
			Double pctMembers = (Double) genotypeResultRow.get(AlignmentAminoAcidFrequencyResult.PERCENTAGE_MEMBERS);
			if(pctMembers >= ravFinder.getGenotypeMaxPct()) {
				continue;
			}
			genotypeVariants.put(new VariantKey(codon, aa), genotypeResultRow);
		}

		Set<VariantKey> intersectionOfKeys = new LinkedHashSet<VariantKey>(samFileVariants.keySet());
		intersectionOfKeys.retainAll(genotypeVariants.keySet());
		
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		for(VariantKey variantKey: intersectionOfKeys) {
			Map<String, Object> row = new LinkedHashMap<String, Object>();
			row.put(FindRavsResult.SAM_FILE_NAME, fileName);
			row.put(FindRavsResult.FEATURE_NAME, featureName);
			row.put(FindRavsResult.CODON, variantKey.codon);
			row.put(FindRavsResult.AMINO_ACID, variantKey.aa);
			row.put(FindRavsResult.GENOTYPE, genotype);
			row.put(FindRavsResult.NUM_READS, samFileVariants.get(variantKey).get(SamAminoAcidResult.READS_WITH_AA));
			row.put(FindRavsResult.PERCENT_READS, samFileVariants.get(variantKey).get(SamAminoAcidResult.PERCENT_AA_READS));
			row.put(FindRavsResult.GENOTYPE_NUM, genotypeVariants.get(variantKey).get(AlignmentAminoAcidFrequencyResult.NUM_MEMBERS));
			row.put(FindRavsResult.GENOTYPE_PCT, genotypeVariants.get(variantKey).get(AlignmentAminoAcidFrequencyResult.PERCENTAGE_MEMBERS));
			rowData.add(row);
		}
		
		return new FindRavsResult(rowData);
	}

	private static class VariantKey {
		int codon;
		String aa;
		public VariantKey(int codon, String aa) {
			super();
			this.codon = codon;
			this.aa = aa;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((aa == null) ? 0 : aa.hashCode());
			result = prime * result + codon;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			VariantKey other = (VariantKey) obj;
			if (aa == null) {
				if (other.aa != null)
					return false;
			} else if (!aa.equals(other.aa))
				return false;
			if (codon != other.codon)
				return false;
			return true;
		}
		
		
	}
	
	
	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
			registerDataObjectNameLookup("featureName", Feature.class, Feature.NAME_PROPERTY);
			registerDataObjectNameLookup("tipRefSource", Source.class, Source.NAME_PROPERTY);
		}
	}
	
	
}
