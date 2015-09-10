package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"set", "metatag"},
		docoptUsages={"<metatagName>"},
		metaTags={CmdMeta.updatesDatabase},
		description="Specify that this feature has a metatag",
		furtherHelp="Metatags are built-in indicators of different feature categories. This command succeeds if the feature already has the metatag."
	) 
public class FeatureSetMetatagCommand extends FeatureModeCommand<CreateResult> {

	public static final String METATAG_NAME = "metatagName";
	private FeatureMetatag.Type metatagType;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		metatagType = PluginUtils.configureEnumProperty(FeatureMetatag.Type.class, configElem, METATAG_NAME, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		Feature feature = lookupFeature(cmdContext);
		FeatureMetatag featureMetatag = 
				GlueDataObject.lookup(cmdContext.getObjectContext(), 
						FeatureMetatag.class, FeatureMetatag.pkMap(feature.getName(), metatagType.name()), true);
		if(featureMetatag != null) {
			return new CreateResult(FeatureMetatag.class, 0);
		}
		featureMetatag = GlueDataObject.create(cmdContext.getObjectContext(), 
				FeatureMetatag.class, FeatureMetatag.pkMap(feature.getName(), metatagType.name()), false);
		featureMetatag.setFeature(feature);
		cmdContext.commit();
		return new CreateResult(FeatureMetatag.class, 1);
	}

	@CompleterClass
	public static class Completer extends MetatagTypeCompleter {}
	
}
