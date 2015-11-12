package uk.ac.gla.cvr.gluetools.core.reporting;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;

@CommandClass(
		commandWords={"preview", "variations"}, 
		description = "Preview variations from members of a single constrained alignment", 
		docoptUsages = { "<alignmentName> [-r] [-w <whereClause>] <referenceName> <featureName>" }, 
		docoptOptions = {
				"-r, --recursive                                Include members of descendent alignments",
				"-w <whereClause>, --whereClause <whereClause>  Qualify included members"},
		furtherHelp = "The alignment named by <alignmentName> must be constrained to a reference. "+
		"The reference sequence named <referenceName> may be the alignment's constraining reference, or "+
		"the constraining reference of any ancestor alignment. The named reference sequence must have a "+
		"location defined for the feature named by <featureName>, this must be a non-informational feature. "+
		"If --recursive is used, the set of alignment members will be expanded to include members of the named "+
		"alignment's child alignments, and those of their child aligments etc. This can therefore be used to generate "+
		"variations from across a whole evolutionary clade. "+
		"If a <whereClause> is supplied, this can qualify further the set of "+
		"included alignment members. Example: \n"+
		"  preview variations AL_3 -w \"sequence.source.name = 'ncbi-curated'\" MREF NS3",
		metaTags = {}	
)
public class PreviewVariationsCommand extends VariationsCommand<PreviewVariationsResult> {

	@Override
	protected PreviewVariationsResult execute(CommandContext cmdContext, MutationFrequenciesReporter modulePlugin) {
		return modulePlugin.previewVariations(cmdContext, 
				getAlignmentName(), isRecursive(), getWhereClause(), getReferenceName(), getFeatureName());
	}

	@CompleterClass
	public static class Completer extends AlignmentAnalysisCommandCompleter {}

}
