package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.PropertyCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.project.PropertyValueResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;


@CommandClass( 
	commandWords={"show", "property"}, 
	docoptUsages={"<property>"},
	description="Show a property value for the variation") 
public class VariationShowPropertyCommand extends VariationModeCommand<PropertyValueResult> {

	public static final String PROPERTY = PropertyCommandDelegate.PROPERTY;
	
	private PropertyCommandDelegate propertyCommandDelegate = new PropertyCommandDelegate();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyCommandDelegate.configureShowProperty(pluginConfigContext, configElem);
	}


	@Override
	public PropertyValueResult execute(CommandContext cmdContext) {
		return propertyCommandDelegate.executeShowProperty(cmdContext);
	}

	@CompleterClass
	public static class Completer extends PropertyCommandDelegate.ListablePropertyCompleter {}


}
