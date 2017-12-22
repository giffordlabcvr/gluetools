package uk.ac.gla.cvr.gluetools.core.command.root.webdocs;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos.WebdocsCommandDocumentation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"webdocs", "document-non-mode-command"}, 
	docoptUsages={"<cmdWordID>"},
	metaTags={CmdMeta.webApiOnly, CmdMeta.suppressDocs}, 
	description = "")
public class WebdocsDocumentNonModeCommandCommand extends WebdocsCommand<PojoCommandResult<WebdocsCommandDocumentation>> {
	
	public static final String CMD_WORD_ID = "cmdWordID";
	
	private String cmdWordID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.cmdWordID = PluginUtils.configureStringProperty(configElem, CMD_WORD_ID, true);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public PojoCommandResult<WebdocsCommandDocumentation> execute(CommandContext cmdContext) {

		WebdocsCommandDocumentation commandDocumentation = new WebdocsCommandDocumentation();
		
		CommandFactory commandFactory = CommandFactory.get(RootCommandFactory.class);
		List<String> commandWords = Arrays.asList(cmdWordID.split("_"));
		Class<? extends Command> cmdClass = commandFactory.identifyCommandClass(cmdContext, commandWords);
		if(cmdClass == null) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Unknown command \""+cmdWordID.replaceAll("_", " ")+"\"");
		}
		commandDocumentation = WebdocsCommandDocumentation.createDocumentation(cmdClass);
		return new PojoCommandResult<WebdocsCommandDocumentation>(commandDocumentation);
	}


}
