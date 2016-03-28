package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.BaseContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.positionVariation.PositionVariation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationFormat;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;


@CommandClass( 
		commandWords={"set","location"}, 
		docoptUsages={"( -n <ntStart> <ntEnd> | -c <lcStart> <lcEnd> )"},
		docoptOptions={
				"-n, --nucleotide     Set location based on reference sequence nucleotide",
				"-c, --labeledCodon   Set location based on labeled codons"},
		metaTags={CmdMeta.updatesDatabase},
		description="Set the variation's location", 
		furtherHelp="The variation's location is the region of a sequence which should be scanned for the variation's pattern. "+
				"The variation's location can be set in different ways. "+ 
				"If the --nucleotide option is used, <ntStart> and <ntEnd> define simply the NT region of the variation's reference sequence, "+
				"using that reference sequence's own coordinates. "+
				"For variations of type AMINO_ACID, the --labeledCodon option may be used. "+
				"In this case labeled codon locations <lcStart>, <lcEnd> are specified using the "+
				"codon-labeling scheme of the variation's feature location.") 
public class VariationSetLocationCommand extends VariationModeCommand<OkResult> {

	public static final String NT_BASED = "nucleotide";
	public static final String NT_START = "ntStart";
	public static final String NT_END = "ntEnd";
	public static final String LC_BASED = "labeledCodon";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";
	
	private Integer ntStart;
	private Integer ntEnd;
	private Boolean nucleotideBased;
	private String lcStart;
	private String lcEnd;
	private Boolean labeledCodonBased;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		ntStart = PluginUtils.configureIntProperty(configElem, NT_START, false);
		ntEnd = PluginUtils.configureIntProperty(configElem, NT_END, false);
		nucleotideBased = PluginUtils.configureBooleanProperty(configElem, NT_BASED, true);
		lcStart = PluginUtils.configureStringProperty(configElem, LC_START, false);
		lcEnd = PluginUtils.configureStringProperty(configElem, LC_END, false);
		labeledCodonBased = PluginUtils.configureBooleanProperty(configElem, LC_BASED, true);
		if(!( 
			(nucleotideBased && !labeledCodonBased && ntStart != null && ntEnd != null && lcStart == null && lcEnd == null) || 
			(!nucleotideBased && labeledCodonBased && ntStart == null && ntEnd == null && lcStart != null && lcEnd != null) ) ) {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Variation location must either be nucleotide or labeled-codon based.");
		}
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		FeatureLocation featureLoc = lookupFeatureLoc(cmdContext);

		Variation variation = lookupVariation(cmdContext);
		TranslationFormat translationFormat = variation.getTranslationFormat();
		Integer codon1Start = null;
		if(translationFormat == TranslationFormat.AMINO_ACID) {
			codon1Start = featureLoc.getCodon1Start(cmdContext);
		}
		
		if(labeledCodonBased) {
			if(translationFormat != TranslationFormat.AMINO_ACID) {
				throw new VariationException(Code.VARIATION_CODON_LOCATION_CAN_NOT_BE_USED_FOR_NUCLEOTIDE_VARIATIONS, 
						getRefSeqName(), getFeatureName(), getVariationName());
			}
			
			Map<String, LabeledCodon> labelToLabeledCodon = featureLoc.getLabelToLabeledCodon(cmdContext);
			LabeledCodon startLabeledCodon = labelToLabeledCodon.get(lcStart);
			if(startLabeledCodon == null) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_LOCATION_OUT_OF_RANGE, 
						getRefSeqName(), getFeatureName(), getVariationName(), lcStart, 
						featureLoc.getFirstLabeledCodon(cmdContext).getCodonLabel(), 
						featureLoc.getLastLabeledCodon(cmdContext).getCodonLabel());
			}
			ntStart = startLabeledCodon.getNtStart();
			LabeledCodon endLabeledCodon = labelToLabeledCodon.get(lcEnd);
			if(endLabeledCodon == null) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_LOCATION_OUT_OF_RANGE, 
						getRefSeqName(), getFeatureName(), getVariationName(), lcEnd, 
						featureLoc.getFirstLabeledCodon(cmdContext).getCodonLabel(), 
						featureLoc.getLastLabeledCodon(cmdContext).getCodonLabel());
			}
			ntEnd = endLabeledCodon.getNtStart()+2;
		}

		
		
		if(ntStart > ntEnd) {
			throw new VariationException(Code.VARIATION_ENDPOINTS_REVERSED, 
					getRefSeqName(), getFeatureName(), getVariationName(), Integer.toString(ntStart), Integer.toString(ntEnd));
		}
		
		Integer oldRefStart = variation.getRefStart();
		Integer oldRefEnd = variation.getRefEnd();
		
		variation.setRefStart(ntStart);
		variation.setRefEnd(ntEnd);
		
		
		Set<Integer> positionsToRemove = new LinkedHashSet<Integer>();
		Set<Integer> positionsToAdd = new LinkedHashSet<Integer>();

		// for amino acid variations we only store position variations at start of codon, for efficiency.
		
		if(oldRefStart != null && oldRefEnd != null) {
			for(int i = oldRefStart; i <= oldRefEnd; i++) {
				if(translationFormat == TranslationFormat.AMINO_ACID && !TranslationUtils.isAtStartOfCodon(codon1Start, i)) {
					continue;
				}
				if(i < ntStart || i > ntEnd) {
					positionsToRemove.add(i);
				}
			}
		}
		for(int i = ntStart; i <= ntEnd; i++) {
			if( (oldRefStart != null && i >= oldRefStart) 
				|| (oldRefEnd != null && i > oldRefEnd) ) {
				continue;
			}
			if(translationFormat == TranslationFormat.AMINO_ACID && !TranslationUtils.isAtStartOfCodon(codon1Start, i)) {
				continue;
			}
			positionsToAdd.add(i);
		}
		for(Integer positionToRemove: positionsToRemove) {
			GlueDataObject.delete(cmdContext, PositionVariation.class, 
					PositionVariation.pkMap(getRefSeqName(), getFeatureName(), getVariationName(), 
							positionToRemove, translationFormat), true);
		}
		for(Integer positionToAdd: positionsToAdd) {
			PositionVariation positionVariation = GlueDataObject.create(cmdContext, PositionVariation.class, 
					PositionVariation.pkMap(getRefSeqName(), getFeatureName(), getVariationName(), 
							positionToAdd, translationFormat), true);
			positionVariation.setFeatureLocation(featureLoc);
			positionVariation.setVariation(variation);
		}
		
		cmdContext.commit();
		((BaseContext) cmdContext.getObjectContext()).getQueryCache().removeGroup(PositionVariation.CACHE_GROUP);
		return new UpdateResult(Variation.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
	}

}
