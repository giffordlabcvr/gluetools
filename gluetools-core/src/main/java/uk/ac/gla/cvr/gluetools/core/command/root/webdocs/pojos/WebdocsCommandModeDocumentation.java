package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandFactory;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class WebdocsCommandModeDocumentation {

	@PojoDocumentField
	public String absoluteModePath;

	@PojoDocumentField
	public String absoluteModePathID;

	@PojoDocumentField
	public String modeDescription;

	@PojoDocumentField
	public String parentModePathID;

	@PojoDocumentField
	public String parentModePath;

	@PojoDocumentField
	public String parentDescription;

	@PojoDocumentListField(itemClass = WebdocsCommandModeSummary.class)
	public List<WebdocsCommandModeSummary> childModes = new ArrayList<WebdocsCommandModeSummary>();

	@PojoDocumentListField(itemClass = WebdocsCommandCategory.class)
	public List<WebdocsCommandCategory> commandCategories = new ArrayList<WebdocsCommandCategory>();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static WebdocsCommandModeDocumentation create(String absoluteModePathID) {
		String[] modePathBits = absoluteModePathID.split("_");
		WebdocsCommandModeDocumentation modeDocumentation = new WebdocsCommandModeDocumentation();
		
		StringBuffer absoluteModePathBuf = new StringBuffer("/");
		String parentModePathID = null;
		if(modePathBits.length > 1) {
			parentModePathID = String.join("_", Arrays.asList(modePathBits).subList(0, modePathBits.length-1));
		}
		String parentModePath = null;
		String parentDescription = null;
		String modeDescription = "Root mode";
		CommandFactory commandFactory = CommandFactory.get(RootCommandFactory.class);
		// start after "root_"..
		for(int i = 1; i < modePathBits.length; i++) {
			String modePathBit = modePathBits[i];
			List<Class<? extends Command>> registeredCmdClasses = commandFactory.getRegisteredCommandClasses();
			Class<? extends Command> enterModeCommandClass = null;
			for(Class<? extends Command> cmdClass: registeredCmdClasses) {
				if(cmdClass.getAnnotation(EnterModeCommandClass.class) != null) {
					String enterModeFirstCmdWord = CommandUsage.cmdWordsForCmdClass(cmdClass)[0];
					if(enterModeFirstCmdWord.equals(modePathBit)) {
						enterModeCommandClass = cmdClass;
						break;
					}
				}
			}
			if(enterModeCommandClass == null) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Unable to identify command mode for path ID \""+absoluteModePathID+"\"");
			}
			EnterModeCommandClass enterModeAnno = enterModeCommandClass.getAnnotation(EnterModeCommandClass.class);
			parentDescription = modeDescription;
			modeDescription = enterModeAnno.modeDescription();
			Class<? extends CommandFactory> commandFactoryClass = enterModeAnno.commandFactoryClass();
			commandFactory = CommandFactory.get(commandFactoryClass);
			String[] modeIDs = CommandUsage.docoptUsagesForCmdClass(enterModeCommandClass)[0].split(" ");
			parentModePath = absoluteModePathBuf.toString();
			absoluteModePathBuf.append(modePathBit+"/"+String.join("/", modeIDs)+"/");
		}

		modeDocumentation.absoluteModePathID = absoluteModePathID;
		modeDocumentation.absoluteModePath = absoluteModePathBuf.toString();
		modeDocumentation.modeDescription = modeDescription;
		modeDocumentation.parentModePath = parentModePath;
		modeDocumentation.parentModePathID = parentModePathID;
		modeDocumentation.parentDescription = parentDescription;
		

		Map<CommandGroup, TreeSet<Class<?>>> cmdGroupToCmdClasses = commandFactory.getCommandGroupRegistry().getCmdGroupToCmdClasses();
		
		cmdGroupToCmdClasses.forEach((cmdGroup, setOfClasses) -> {
			if(cmdGroup.isNonModeSpecific()) {
				return;
			}
			List<WebdocsCommandSummary> commandSummaries = new ArrayList<WebdocsCommandSummary>();
			setOfClasses.forEach(cmdClass -> {
				if(CommandUsage.hasMetaTagForCmdClass((Class<? extends Command>) cmdClass, CmdMeta.suppressDocs)) {
					return;
				}
				commandSummaries.add(WebdocsCommandSummary.createSummary((Class<? extends Command>) cmdClass));
				EnterModeCommandClass enterModeAnno = cmdClass.getAnnotation(EnterModeCommandClass.class);
				if(enterModeAnno != null) {
					String[] modeIDs = CommandUsage.docoptUsagesForCmdClass((Class<? extends Command>) cmdClass)[0].split(" ");
					String enterModeFirstCmdWord = CommandUsage.cmdWordsForCmdClass((Class<? extends Command>) cmdClass)[0];
					WebdocsCommandModeSummary modeSummary = new WebdocsCommandModeSummary();
					modeSummary.description = enterModeAnno.modeDescription();
					modeSummary.modePathID = modeDocumentation.absoluteModePathID+"_"+enterModeFirstCmdWord;
					modeSummary.modePath = modeDocumentation.absoluteModePath+enterModeFirstCmdWord+"/"+String.join("/", modeIDs)+"/";
					modeDocumentation.childModes.add(modeSummary);
				}
				
				
			});
			if(!commandSummaries.isEmpty()) {
				modeDocumentation.commandCategories.add(WebdocsCommandCategory.create(cmdGroup.getId(), cmdGroup.getDescription(), commandSummaries));
			}
		});
		
		return modeDocumentation;
	}
	
}
