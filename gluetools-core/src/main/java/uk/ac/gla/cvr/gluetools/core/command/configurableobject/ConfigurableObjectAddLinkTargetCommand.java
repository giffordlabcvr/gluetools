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
	commandWords={"add", "link-target"}, 
	docoptUsages={"[-C] <linkName> <targetPath>"},
	docoptOptions={
			"-C, --noCommit     Don't commit to the database [default: false]",
	},
	metaTags={CmdMeta.updatesDatabase},
	description="Add a target object on a link") 
public class ConfigurableObjectAddLinkTargetCommand extends Command<UpdateResult> {

	public static final String LINK_NAME = PropertyCommandDelegate.LINK_NAME;
	public static final String TARGET_PATH = PropertyCommandDelegate.TARGET_PATH;
	public static final String NO_COMMIT = PropertyCommandDelegate.NO_COMMIT;

	
	private PropertyCommandDelegate propertyCommandDelegate = new PropertyCommandDelegate();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyCommandDelegate.configureAddLinkTarget(pluginConfigContext, configElem);
	}

	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		return propertyCommandDelegate.executeAddLinkTarget(cmdContext);
	}

	@CompleterClass
	public static class Completer extends PropertyCommandDelegate.ToManyLinkNameAndTargetPathCompleter {}

}