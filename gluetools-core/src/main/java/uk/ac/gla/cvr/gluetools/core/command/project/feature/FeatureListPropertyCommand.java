package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ListPropertyResult;
import uk.ac.gla.cvr.gluetools.core.command.project.PropertyCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;


@CommandClass( 
	commandWords={"list", "property"}, 
	docoptUsages={""},
	description="List property values for the feature") 
public class FeatureListPropertyCommand extends FeatureModeCommand<ListPropertyResult> {

	private PropertyCommandDelegate propertyCommandDelegate = new PropertyCommandDelegate();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyCommandDelegate.configureListProperty(pluginConfigContext, configElem);
	}


	@Override
	public ListPropertyResult execute(CommandContext cmdContext) {
		return propertyCommandDelegate.executeListProperty(cmdContext);
	}

}
