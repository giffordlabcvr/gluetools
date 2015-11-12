package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.CreateVariationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationAddCategoryCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationSetLocationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationSetPatternCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;import uk.ac.gla.cvr.gluetools.core.reporting.MutationFrequenciesException.Code;




@CommandClass(
		commandWords={"generate", "variations"}, 
		description = "Generate variations from members of a single constrained alignment", 
		docoptUsages = { "<alignmentName> [-r] [-w <whereClause>] <referenceName> <featureName> [-c <vcatName> ...]" }, 
		docoptOptions = {
				"-r, --recursive                                Include members of descendent alignments",
				"-w <whereClause>, --whereClause <whereClause>  Qualify included members",
				"-c <vcatName>, --categories <vcatName>         Add variation category"},
		furtherHelp = "The alignment named by <alignmentName> must be constrained to a reference. "+
		"The reference sequence named <referenceName> may be the alignment's constraining reference, or "+
		"the constraining reference of any ancestor alignment. The named reference sequence must have a "+
		"location defined for the feature named by <featureName>, this must be a non-informational feature. "+
		"If --recursive is used, the set of alignment members will be expanded to include members of the named "+
		"alignment's child alignments, and those of their child aligments etc. This can therefore be used to generate "+
		"variations from across a whole evolutionary clade. "+
		"If a <whereClause> is supplied, this can qualify further the set of "+
		"included alignment members. Example: \n"+
		"  generate variations AL_3 -w \"sequence.source.name = 'ncbi-curated'\" MREF NS3\n"+
		"One or more named variation categories may be specified, using the --vcatName option. If so, the generated "+
		"variations are added to the named categories",
		metaTags = {CmdMeta.updatesDatabase}	
)
public class GenerateVariationsCommand extends VariationsCommand<GenerateVariationsResult> {

	private List<String> vcatNames;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		vcatNames = PluginUtils.configureStringsProperty(configElem, "categories");
	}

	@Override
	protected GenerateVariationsResult execute(CommandContext cmdContext, MutationFrequenciesReporter modulePlugin) {
		PreviewVariationsResult previewVariationsResult = modulePlugin.previewVariations(cmdContext, 
				getAlignmentName(), isRecursive(), getWhereClause(), getReferenceName(), getFeatureName());
		// lookup variation categories in advance.
		vcatNames.forEach(vcatName -> {
			GlueDataObject.lookup(cmdContext, VariationCategory.class, VariationCategory.pkMap(vcatName));
		});
		final String vcatTableString;
		if(vcatNames.isEmpty()) {
			vcatTableString = null;
		} else {
			vcatTableString = String.join(", ", vcatNames);
		}
		List<Map<String, Object>> listOfMaps = previewVariationsResult.asListOfMaps();
		
		try ( ModeCloser refSeqMode = cmdContext.pushCommandMode("reference", getReferenceName() ) ) {
			try ( ModeCloser featureLocationMode = cmdContext.pushCommandMode("feature-location", getFeatureName() ) ) {
				listOfMaps.forEach(row -> {
					String variationName = (String) row.get(PreviewVariationsResult.VARIATION_NAME);
					Integer refStart = (Integer) row.get(PreviewVariationsResult.REF_START);
					Integer refEnd = (Integer) row.get(PreviewVariationsResult.REF_END);
					String regex = (String) row.get(PreviewVariationsResult.REGEX);
					String translationType = (String) row.get(PreviewVariationsResult.TRANSLATION_FORMAT);
					String vcatNamesString = vcatTableString;
					
					boolean alreadyExists = false;
					Variation existingVariation = null;
					if(modulePlugin.mergeGeneratedVariations()) {
						existingVariation = GlueDataObject.lookup(cmdContext, Variation.class, 
								Variation.pkMap(getReferenceName(), getFeatureName(), variationName), true);
						if(existingVariation != null) {
							if(existingVariation.getRegex().equals(regex) &&
									existingVariation.getRefStart().equals(refStart) &&
									existingVariation.getRefEnd().equals(refEnd) &&
									existingVariation.getTranslationFormat().name().equals(translationType)) {
								alreadyExists = true;
								List<String> existingVcatNames = existingVariation.getVcatMemberships().stream().map(vcm -> vcm.getCategory().getName()).collect(Collectors.toList());
								Set<String> updatedVcatNames = new LinkedHashSet<String>(existingVcatNames);
								updatedVcatNames.addAll(vcatNames);
								vcatNamesString = String.join(", ", updatedVcatNames);
							} else {
								throw new MutationFrequenciesException(Code.VARIATION_CANNOT_BE_MERGED, getReferenceName(), getFeatureName(), variationName);
							}
						}
					}
					
					if(!alreadyExists) {
						cmdContext.cmdBuilder(CreateVariationCommand.class)
						.set(CreateVariationCommand.VARIATON_NAME, variationName)
						.execute();
					}
					try ( ModeCloser variationMode = cmdContext.pushCommandMode("variation", variationName ) ) {
						if(!alreadyExists) {
							cmdContext.cmdBuilder(VariationSetLocationCommand.class)
							.set(VariationSetLocationCommand.REF_START, refStart)
							.set(VariationSetLocationCommand.REF_END, refEnd)
							.execute();
							cmdContext.cmdBuilder(VariationSetPatternCommand.class)
							.set(VariationSetPatternCommand.REGEX, regex)
							.set(VariationSetPatternCommand.TRANSLATION_TYPE, translationType)
							.execute();
						}
						for(String vcatName: vcatNames) {
							cmdContext.cmdBuilder(VariationAddCategoryCommand.class)
							.set(VariationAddCategoryCommand.VCAT_NAME, vcatName)
							.execute();
						}
					}
					row.put(GenerateVariationsResult.MERGE, new Boolean(alreadyExists));
					row.put(GenerateVariationsResult.VARIATION_CATEGORIES, vcatNamesString);
				});
			}
		}
		return new GenerateVariationsResult(listOfMaps);
	}

	@CompleterClass
	public static class Completer extends AlignmentAnalysisCommandCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("vcatName", new VariableInstantiator() {
				@Override
				@SuppressWarnings({ "rawtypes", "unchecked" }) 
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					Set<String> current = new LinkedHashSet<String>();
					List<String> currentList = (List<String>) bindings.get("vcatName");
					if(currentList != null) {
						current.addAll(currentList);
					}
					List<CompletionSuggestion> allVCatNames = AdvancedCmdCompleter.listNames(cmdContext, prefix, VariationCategory.class, VariationCategory.NAME_PROPERTY);
					return allVCatNames.stream().filter(cs -> !current.contains(cs.getSuggestedWord())).collect(Collectors.toList());
				}
			});
		}
		
	}

}
