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
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos.WebdocsCommandDocumentation;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos.WebdocsModeCommandDocumentation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"webdocs", "document-mode-command"}, 
	docoptUsages={"<absoluteModePathID> <cmdWordID>"},
	metaTags={CmdMeta.webApiOnly, CmdMeta.suppressDocs}, 
	description = "")
public class WebdocsDocumentModeCommandCommand extends WebdocsCommand<PojoCommandResult<WebdocsModeCommandDocumentation>> {
	
	
	public static final String ABSOLUTE_MODE_PATH_ID = "absoluteModePathID";
	public static final String CMD_WORD_ID = "cmdWordID";
	
	private String absoluteModePathID;
	private String cmdWordID;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.absoluteModePathID = PluginUtils.configureStringProperty(configElem, ABSOLUTE_MODE_PATH_ID, true);
		this.cmdWordID = PluginUtils.configureStringProperty(configElem, CMD_WORD_ID, true);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public PojoCommandResult<WebdocsModeCommandDocumentation> execute(CommandContext cmdContext) {

		WebdocsModeCommandDocumentation modeCommandDocumentation = new WebdocsModeCommandDocumentation();
		
		String[] modePathBits = absoluteModePathID.split("_");
		String modeDescription = "Root mode";
		CommandFactory commandFactory = CommandFactory.get(RootCommandFactory.class);
		// start after "root_"..
		for(int i = 1; i < modePathBits.length; i++) {
			String modePathBit = modePathBits[i];
			List<Class<? extends Command>> registeredCmdClasses = commandFactory.getRegisteredCommandClasses();
			Class<? extends Command> enterModeCommandClass = null;
			for(Class<? extends Command> registeredCmdClass: registeredCmdClasses) {
				if(registeredCmdClass.getAnnotation(EnterModeCommandClass.class) != null) {
					String enterModeFirstCmdWord = CommandUsage.cmdWordsForCmdClass(registeredCmdClass)[0];
					if(enterModeFirstCmdWord.equals(modePathBit)) {
						enterModeCommandClass = registeredCmdClass;
						break;
					}
				}
			}
			if(enterModeCommandClass == null) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Unable to identify command mode for path ID \""+absoluteModePathID+"\"");
			}
			EnterModeCommandClass enterModeAnno = enterModeCommandClass.getAnnotation(EnterModeCommandClass.class);
			Class<? extends CommandFactory> commandFactoryClass = enterModeAnno.commandFactoryClass();
			modeDescription = enterModeAnno.modeDescription();
			commandFactory = CommandFactory.get(commandFactoryClass);
		}		
		List<String> commandWords = Arrays.asList(cmdWordID.split("_"));
		Class<? extends Command> cmdClass = commandFactory.identifyCommandClass(cmdContext, commandWords);
		if(cmdClass == null) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Unknown command \""+cmdWordID.replaceAll("_", " ")+"\"");
		}
		modeCommandDocumentation.absoluteModePathID = absoluteModePathID;
		modeCommandDocumentation.modeDescription = modeDescription;
		modeCommandDocumentation.commandDocumentation = WebdocsCommandDocumentation.createDocumentation(cmdClass);
		return new PojoCommandResult<WebdocsModeCommandDocumentation>(modeCommandDocumentation);
	}

}
