package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
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
		Feature feature = lookupFeature(cmdContext);
		Feature parentFeature = GlueDataObject.lookup(cmdContext.getObjectContext(), Feature.class, Feature.pkMap(parentFeatureName));
		feature.setParent(parentFeature);
		cmdContext.commit();
		return new OkResult();
	}

	@CompleterClass
	public static class FeatureNameCompleter extends ProjectModeCommand.FeatureNameCompleter {}
	
}