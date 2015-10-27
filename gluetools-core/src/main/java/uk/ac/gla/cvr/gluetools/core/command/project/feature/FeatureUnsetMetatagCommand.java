package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"unset", "metatag"},
		docoptUsages={"<metatagName>"},
		metaTags={CmdMeta.updatesDatabase},
		description="Specify that this feature does not have a metatag",
		furtherHelp="This command succeeds if the feature already does not have the metatag."
	) 
public class FeatureUnsetMetatagCommand extends FeatureModeCommand<DeleteResult> {

	public static final String METATAG_NAME = "metatagName";
	private FeatureMetatag.Type metatagType;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		metatagType = PluginUtils.configureEnumProperty(FeatureMetatag.Type.class, configElem, METATAG_NAME, true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		Feature feature = lookupFeature(cmdContext);
		DeleteResult result = GlueDataObject.delete(cmdContext, 
				FeatureMetatag.class, FeatureMetatag.pkMap(feature.getName(), metatagType.name()), true);
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends MetatagTypeCompleter {}
	
}