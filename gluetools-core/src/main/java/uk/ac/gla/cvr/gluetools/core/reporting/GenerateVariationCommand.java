package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.CreateVariationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationAddCategoryCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationSetLocationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationSetPatternCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.AlignmentAnalysisCommandCompleter.VcatNameCompleter;
import uk.ac.gla.cvr.gluetools.core.reporting.MutationFrequenciesException.Code;




@CommandClass(
		commandWords={"generate", "variation"}, 
		description = "Generate variation representing single amino acid mutation", 
		docoptUsages = { "<referenceName> <featureName> <codon> <mutationAA> [-c <vcatName> ...]" }, 
		docoptOptions = {
				"-c <vcatName>, --categories <vcatName>         Add variation category"},
		furtherHelp = "The alignment named by <alignmentName> must be constrained to a reference. "+
		"The reference sequence named <referenceName> may be the alignment's constraining reference, or "+
		"the constraining reference of any ancestor alignment. The named reference sequence must have a "+
		"location defined for the feature named by <featureName>, this must be a non-informational feature. "+
		"One or more named variation categories may be specified, using the --vcatName option. If so, the generated "+
		"variations are added to the named categories\n"+
		"Example: \n"+
		"  generate variation REF_A NS2 98 L -c common_in_genoA\n",
		metaTags = {CmdMeta.updatesDatabase}	
)
public class GenerateVariationCommand extends ModulePluginCommand<GenerateVariationsResult, MutationFrequenciesReporter> implements ProvidedProjectModeCommand {

	public static final String REFERENCE_NAME = "referenceName";
	public static final String FEATURE_NAME = "featureName";
	public static final String CODON = "codon";
	public static final String MUTATION_AA = "mutationAA";

	private List<String> vcatNames;
	private String referenceName;
	private String featureName;
	private Integer codon;
	private String mutationAA;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		codon = PluginUtils.configureIntProperty(configElem, CODON, true);
		vcatNames = PluginUtils.configureStringsProperty(configElem, "categories");
		mutationAA = PluginUtils.configureStringProperty(configElem, MUTATION_AA, true);
	}

	@Override
	protected GenerateVariationsResult execute(CommandContext cmdContext, MutationFrequenciesReporter modulePlugin) {
		PreviewVariationsResult previewVariationsResult = 
				modulePlugin.previewSingleCodonVariation(cmdContext, referenceName, featureName, codon, mutationAA);
		return generateVariationsFromPreview(cmdContext, modulePlugin, previewVariationsResult, vcatNames, referenceName, featureName);
	}
	
	public static GenerateVariationsResult generateVariationsFromPreview(
			CommandContext cmdContext,
			MutationFrequenciesReporter modulePlugin,
			PreviewVariationsResult previewVariationsResult,
			List<String> vcatNames, String referenceName, String featureName) {
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
		
		try ( ModeCloser refSeqMode = cmdContext.pushCommandMode("reference", referenceName ) ) {
			try ( ModeCloser featureLocationMode = cmdContext.pushCommandMode("feature-location", featureName ) ) {
				listOfMaps.forEach(row -> {
					String variationName = (String) row.get(PreviewVariationsResult.VARIATION_NAME);
					String variationDescription = (String) row.get(PreviewVariationsResult.VARIATION_DESCRIPTION);
					Integer refStart = (Integer) row.get(PreviewVariationsResult.REF_START);
					Integer refEnd = (Integer) row.get(PreviewVariationsResult.REF_END);
					String regex = (String) row.get(PreviewVariationsResult.REGEX);
					String translationType = (String) row.get(PreviewVariationsResult.TRANSLATION_FORMAT);
					String vcatNamesString = vcatTableString;
					
					boolean alreadyExists = false;
					Variation existingVariation = null;
					if(modulePlugin.mergeGeneratedVariations()) {
						existingVariation = GlueDataObject.lookup(cmdContext, Variation.class, 
								Variation.pkMap(referenceName, featureName, variationName), true);
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
								throw new MutationFrequenciesException(Code.VARIATION_CANNOT_BE_MERGED, referenceName, featureName, variationName);
							}
						}
					}
					
					if(!alreadyExists) {
						CommandBuilder<CreateResult, CreateVariationCommand> cmdBuilder = 
							cmdContext.cmdBuilder(CreateVariationCommand.class)
							.set(CreateVariationCommand.VARIATON_NAME, variationName)
							.set(CreateVariationCommand.TRANSLATION_TYPE, translationType);
						if(variationDescription != null) {
							cmdBuilder.set(CreateVariationCommand.DESCRIPTION,  variationDescription);
						}
						cmdBuilder.execute();
					}
					try ( ModeCloser variationMode = cmdContext.pushCommandMode("variation", variationName ) ) {
						if(!alreadyExists) {
							cmdContext.cmdBuilder(VariationSetLocationCommand.class)
							.set(VariationSetLocationCommand.NT_START, refStart)
							.set(VariationSetLocationCommand.NT_END, refEnd)
							.execute();
							cmdContext.cmdBuilder(VariationSetPatternCommand.class)
							.set(VariationSetPatternCommand.REGEX, regex)
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
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("referenceName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String referenceName = (String) bindings.get("referenceName");
					ReferenceSequence refSeq = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(referenceName), true);
					if(refSeq != null) {
						List<FeatureLocation> featureLocations = refSeq.getFeatureLocations();
						List<CompletionSuggestion> suggestions = new ArrayList<CompletionSuggestion>();
						for(FeatureLocation featureLoc: featureLocations) {
							Feature feature = featureLoc.getFeature();
							if(!feature.isInformational() && feature.getOrfAncestor() != null) {
								suggestions.add(new CompletionSuggestion(feature.getName(), true));
							}
						}
						return suggestions;
					}
					return null;
				}
			});
			registerVariableInstantiator("vcatName", new VcatNameCompleter());
		}
	}
}
