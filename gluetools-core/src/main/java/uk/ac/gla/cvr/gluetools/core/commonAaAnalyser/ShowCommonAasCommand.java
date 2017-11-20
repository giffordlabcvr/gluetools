package uk.ac.gla.cvr.gluetools.core.commonAaAnalyser;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;

@CommandClass(
		commandWords={"show", "common-aas"}, 
		description = "Show the common amino-acids at different codon locations", 
		docoptUsages={"<alignmentName> -r <acRefName> -f <featureName> [-c] (-w <whereClause> | -a)"},
		docCategory = "Type-specific module commands",
		docoptOptions={
			"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
			"-f <featureName>, --featureName <featureName>  Protein-coding feature",
			"-c, --recursive                                Include descendent members", 
			"-w <whereClause>, --whereClause <whereClause>  Qualify members",
		    "-a, --allMembers                               All members"},
		furtherHelp = ""
)
public class ShowCommonAasCommand extends AbstractAnalyseAasCommand<CommonAasResult> {


	
	@Override
	protected CommonAasResult execute(CommandContext cmdContext, CommonAaAnalyser commonAaAnalyser) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(getAlignmentName()));
		alignment.getAncConstrainingRef(cmdContext, getAcRefName());
		List<CommonAminoAcids> commonAas = 
				commonAaAnalyser.commonAas(cmdContext, getAlignmentName(), getAcRefName(), getFeatureName(), getWhereClause(), getRecursive());
		return new CommonAasResult(commonAas);
	}

	@CompleterClass 
	public static class Completer extends AnalyseAasCompleter {
	}
	
}


