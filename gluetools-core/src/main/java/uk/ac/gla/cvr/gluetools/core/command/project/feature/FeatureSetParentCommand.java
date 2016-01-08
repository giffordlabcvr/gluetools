package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.ExpressionFactory;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"set", "parent"},
		docoptUsages={"<parentFeatureName>"},
		metaTags={CmdMeta.updatesDatabase},
		description="Specify the parent for this feature",
		furtherHelp="Loops arising from feature parent relationships are not allowed."
	) 
public class FeatureSetParentCommand extends FeatureModeCommand<OkResult> {

	public static final String PARENT_FEATURE_NAME = "parentFeatureName";
	private String parentFeatureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		parentFeatureName = PluginUtils.configureStringProperty(configElem, PARENT_FEATURE_NAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		String inferOrderValue = cmdContext.getProjectSettingValue(ProjectSettingOption.INFER_FEATURE_DISPLAY_ORDER);
		if(inferOrderValue.equals("true")) {
			GlueLogger.getGlueLogger().warning("Since project setting "+ProjectSettingOption.INFER_FEATURE_DISPLAY_ORDER+" is in use, parent should be set when the feature is created.");
		}
		Feature feature = lookupFeature(cmdContext);
		Feature parentFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(parentFeatureName));
		feature.setParent(parentFeature);
		cmdContext.commit();
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {

		public Completer() {
			super();
			registerVariableInstantiator("parentFeatureName", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					String thisFeatureName = ((FeatureMode) cmdContext.peekCommandMode()).getFeatureName();
					return listNames(cmdContext, prefix, Feature.class, Feature.NAME_PROPERTY, 
							ExpressionFactory.noMatchExp(Feature.NAME_PROPERTY, thisFeatureName));
				}
			});

		}
		
		
	}
	
}
