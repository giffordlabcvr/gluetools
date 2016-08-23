package uk.ac.gla.cvr.gluetools.core.command.configurableobject;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;


@CommandClass( 
	commandWords={"list", "link-target"}, 
	docoptUsages={"<linkName>"},
	docoptOptions={},
	metaTags={CmdMeta.updatesDatabase},
	description="List target objects on a link") 
public class ConfigurableObjectListLinkTargetCommand extends Command<ListResult> {

	public static final String LINK_NAME = PropertyCommandDelegate.LINK_NAME;

	
	private PropertyCommandDelegate propertyCommandDelegate = new PropertyCommandDelegate();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyCommandDelegate.configureListLinkTarget(pluginConfigContext, configElem);
	}

	@Override
	public ListResult execute(CommandContext cmdContext) {
		return propertyCommandDelegate.executeListLinkTarget(cmdContext);
	}

	@CompleterClass
	public static class Completer extends PropertyCommandDelegate.ToManyLinkNameAndTargetPathCompleter {}

}
