package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.List;


import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.CreateVariationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationAddCategoryCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationSetLocationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationSetPatternCommand;



@CommandClass(
		commandWords={"generate", "variations"}, 
		description = "Generate variations from members of a single constrained alignment", 
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
		"  generate variations AL_3 -w \"sequence.source.name = 'ncbi-curated'\" MREF NS3",
		metaTags = {CmdMeta.updatesDatabase}	
)
public class GenerateVariationsCommand extends VariationsCommand {

	@Override
	protected PreviewVariationsResult execute(CommandContext cmdContext, MutationFrequenciesReporter modulePlugin) {
		PreviewVariationsResult previewVariationsResult = modulePlugin.previewVariations(cmdContext, 
				getAlignmentName(), isRecursive(), getWhereClause(), getReferenceName(), getFeatureName());
		List<String> generatedVariationCategories = modulePlugin.getGeneratedVariationCategories();
		try ( ModeCloser refSeqMode = cmdContext.pushCommandMode("reference", getReferenceName() ) ) {
			try ( ModeCloser featureLocationMode = cmdContext.pushCommandMode("feature-location", getFeatureName() ) ) {
				previewVariationsResult.asListOfMaps().forEach(row -> {
					String variationName = (String) row.get(PreviewVariationsResult.VARIATION_NAME);
					Integer refStart = (Integer) row.get(PreviewVariationsResult.REF_START);
					Integer refEnd = (Integer) row.get(PreviewVariationsResult.REF_END);
					String regex = (String) row.get(PreviewVariationsResult.REGEX);
					String translationType = (String) row.get(PreviewVariationsResult.TRANSLATION_FORMAT);
					
					cmdContext.cmdBuilder(CreateVariationCommand.class)
						.set(CreateVariationCommand.VARIATON_NAME, variationName)
						.execute();
					try ( ModeCloser variationMode = cmdContext.pushCommandMode("variation", variationName ) ) {
						cmdContext.cmdBuilder(VariationSetLocationCommand.class)
						.set(VariationSetLocationCommand.REF_START, refStart)
						.set(VariationSetLocationCommand.REF_END, refEnd)
						.execute();
						cmdContext.cmdBuilder(VariationSetPatternCommand.class)
						.set(VariationSetPatternCommand.REGEX, regex)
						.set(VariationSetPatternCommand.TRANSLATION_TYPE, translationType)
						.execute();
						for(String vcatName: generatedVariationCategories) {
							cmdContext.cmdBuilder(VariationAddCategoryCommand.class)
							.set(VariationAddCategoryCommand.VCAT_NAME, vcatName)
							.execute();
						}
					}
					
				});
			}
		}
		return previewVariationsResult;
	}

	@CompleterClass
	public static class Completer extends AlignmentAnalysisCommandCompleter {}

}
