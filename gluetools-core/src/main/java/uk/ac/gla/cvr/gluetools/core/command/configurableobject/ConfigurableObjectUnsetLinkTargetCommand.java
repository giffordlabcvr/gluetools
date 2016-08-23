package uk.ac.gla.cvr.gluetools.core.command.configurableobject;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;


@CommandClass( 
	commandWords={"unset", "link-target"}, 
	docoptUsages={"[-C] <linkName>"},
	docoptOptions={
			"-C, --noCommit     Don't commit to the database [default: false]",
	},
	metaTags={CmdMeta.updatesDatabase},
	description="Unset the target object on a link",
	furtherHelp="After the command has executed, the current mode object will have no target for the specified link.") 
public class ConfigurableObjectUnsetLinkTargetCommand extends Command<UpdateResult> {

	public static final String LINK_NAME = PropertyCommandDelegate.LINK_NAME;
	public static final String NO_COMMIT = PropertyCommandDelegate.NO_COMMIT;
	
	private PropertyCommandDelegate propertyCommandDelegate = new PropertyCommandDelegate();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyCommandDelegate.configureUnsetLinkTarget(pluginConfigContext, configElem);
	}

	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		return propertyCommandDelegate.executeUnsetLinkTarget(cmdContext);
	}

	@CompleterClass
	public static class Completer extends PropertyCommandDelegate.ToOneLinkNameAndTargetPathCompleter {}

}
