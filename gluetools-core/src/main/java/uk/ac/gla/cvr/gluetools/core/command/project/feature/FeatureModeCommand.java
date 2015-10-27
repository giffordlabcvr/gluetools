package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class FeatureModeCommand<R extends CommandResult> extends ProjectModeCommand<R> {


	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
	}

	protected String getFeatureName() {
		return featureName;
	}


	protected static FeatureMode getFeatureMode(CommandContext cmdContext) {
		return (FeatureMode) cmdContext.peekCommandMode();
	}


	protected Feature lookupFeature(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, Feature.class, 
				Feature.pkMap(getFeatureName()));
	}

	
	@CompleterClass
	public static class MetatagTypeCompleter extends AdvancedCmdCompleter {

		public MetatagTypeCompleter() {
			super();
			registerEnumLookup("metatagName", FeatureMetatag.Type.class);
		}

		
	}

}
