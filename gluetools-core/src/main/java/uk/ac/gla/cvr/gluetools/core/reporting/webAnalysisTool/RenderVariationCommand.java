package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool.WebAnalysisException.Code;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils.GlueDataObjectTemplateModel;

@CommandClass(
		commandWords={"render", "variation"}, 
		description = "Render a variation", 
		docoptUsages = { "<categoryName> <referenceName> <featureName> <variationName>" },
		docoptOptions = { },
		metaTags = { }	
)
public class RenderVariationCommand extends ModulePluginCommand<RenderVariationCommand.RenderVariationResult, WebAnalysisTool> {
	

	private String categoryName;
	private String referenceName;
	private String featureName;
	private String variationName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.categoryName = PluginUtils.configureStringProperty(configElem, "categoryName", true);
		this.referenceName = PluginUtils.configureStringProperty(configElem, "referenceName", true);
		this.featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
		this.variationName = PluginUtils.configureStringProperty(configElem, "variationName", true);
	}

	@Override
	protected RenderVariationResult execute(CommandContext cmdContext, WebAnalysisTool webAnalysisTool) {
		VariationCategory variationCategory = webAnalysisTool.getVariationCategory(categoryName);
		if(variationCategory == null) {
			throw new WebAnalysisException(Code.UNKNOWN_VARIATION_CATEGORY, categoryName);
		}
		Variation variation = GlueDataObject.lookup(cmdContext, Variation.class, 
				Variation.pkMap(referenceName, featureName, variationName));
		
		GlueDataObjectTemplateModel templateModel = new GlueDataObjectTemplateModel(variation);
		Map<String, Object> renderResult = new LinkedHashMap<String, Object>();

		for(PropertyTemplate propertyTemplate: variationCategory.getPropertyTemplates()) {
			String name = propertyTemplate.getName();
			String value = FreemarkerUtils.processTemplate(propertyTemplate.getTemplate(), templateModel);
			renderResult.put(name, value);
		}
		
		return new RenderVariationResult(renderResult);
	}

	public static class RenderVariationResult extends MapResult {

		public RenderVariationResult(Map<String, Object> renderResult) {
			super("renderVariationResult", renderResult);
		}
		
	}

	@CompleterClass 
	public static class Completer extends ModuleCmdCompleter<WebAnalysisTool> {
		public Completer() {
			super();
			registerVariableInstantiator("categoryName", new ModuleVariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						WebAnalysisTool webAnalysisTool,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return webAnalysisTool.getVariationCategories().stream()
							.map(vcat -> new CompletionSuggestion(vcat.getName(), true))
							.collect(Collectors.toList());
				}
			});
			registerDataObjectNameLookup("referenceName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
			registerVariableInstantiator("featureName", 
					new QualifiedDataObjectNameInstantiator(FeatureLocation.class, FeatureLocation.FEATURE_NAME_PATH) {
				@Override
				protected void qualifyResults(@SuppressWarnings("rawtypes") CommandMode cmdMode,
						Map<String, Object> bindings, Map<String, Object> qualifierValues) {
					String refSeqName = (String) bindings.get("referenceName");
					qualifierValues.put(FeatureLocation.REF_SEQ_NAME_PATH, refSeqName);
				}
			});
			registerVariableInstantiator("variationName", new QualifiedDataObjectNameInstantiator(
					Variation.class, Variation.NAME_PROPERTY) {
				@Override
				protected void qualifyResults(@SuppressWarnings("rawtypes") CommandMode cmdMode,
						Map<String, Object> bindings, Map<String, Object> qualifierValues) {
					String refSeqName = (String) bindings.get("referenceName");
					String featureName = (String) bindings.get("featureName");
					qualifierValues.put(Variation.REF_SEQ_NAME_PATH, refSeqName);
					qualifierValues.put(Variation.FEATURE_NAME_PATH, featureName);
				}
			});
		}
		
	}
	

}
