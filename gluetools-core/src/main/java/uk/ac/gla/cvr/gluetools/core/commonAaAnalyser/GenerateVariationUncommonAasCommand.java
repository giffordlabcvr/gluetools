package uk.ac.gla.cvr.gluetools.core.commonAaAnalyser;

import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

@CommandClass(
		commandWords={"generate", "variation", "uncommon-aas"}, 
		description = "Generate variations for detecting uncommon amino acids", 
		docoptUsages={"<alignmentName> -r <acRefName> -f <featureName> [-c] (-w <whereClause> | -a)"},
		docoptOptions={
			"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
			"-f <featureName>, --featureName <featureName>  Protein-coding feature",
			"-c, --recursive                                Include descendent members", 
			"-w <whereClause>, --whereClause <whereClause>  Qualify members",
		    "-a, --allMembers                               All members"},
		metaTags = {CmdMeta.updatesDatabase}, 
		furtherHelp = ""
)
public class GenerateVariationUncommonAasCommand extends AbstractAnalyseAasCommand<CreateResult> {
	
	@Override
	protected CreateResult execute(CommandContext cmdContext, CommonAaAnalyser commonAaAnalyser) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(getAlignmentName()));
		alignment.getAncConstrainingRef(cmdContext, getAcRefName());
		ReferenceSequence ancConstrainingRef = alignment.getAncConstrainingRef(cmdContext, getAcRefName());

		List<CommonAminoAcids> commonAas = commonAaAnalyser.commonAas(cmdContext, alignment, ancConstrainingRef, getFeatureName(), getWhereClause(), getRecursive());
		
		List<Map<String,String>> variationPkMaps = commonAaAnalyser.generateVariationUncommonAas(cmdContext, commonAas);
		
		return new CreateResult(Variation.class, variationPkMaps.size());
	}

	@CompleterClass 
	public static class Completer extends AnalyseAasCompleter {
	}
	
}


