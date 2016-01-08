package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"set", "metatag"},
		docoptUsages={"<metatagName> <metatagValue>"},
		metaTags={CmdMeta.updatesDatabase},
		description="Add or update a metatag with a certain name/value",
		furtherHelp="Metatags are metadata for features."
	) 
public class FeatureSetMetatagCommand extends FeatureModeCommand<OkResult> {

	public static final String METATAG_NAME = "metatagName";
	public static final String METATAG_VALUE = "metatagValue";

	private FeatureMetatag.Type metatagType;
	private String metatagValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		metatagType = PluginUtils.configureEnumProperty(FeatureMetatag.Type.class, configElem, METATAG_NAME, true);
		metatagValue = PluginUtils.configureStringProperty(configElem, METATAG_VALUE, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Feature feature = lookupFeature(cmdContext);
		FeatureMetatag featureMetatag = 
				GlueDataObject.lookup(cmdContext, 
						FeatureMetatag.class, FeatureMetatag.pkMap(feature.getName(), metatagType.name()), true);
		if(featureMetatag == null) {
			featureMetatag = GlueDataObject.create(cmdContext, 
					FeatureMetatag.class, FeatureMetatag.pkMap(feature.getName(), metatagType.name()), false);
			featureMetatag.setFeature(feature);
		}
		featureMetatag.setValue(metatagValue);
		cmdContext.commit();
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends MetatagTypeCompleter {}
	
}
