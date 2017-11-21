package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class WebdocsCommandModeTree {

	@PojoDocumentField
	public String relativeModePath;

	@PojoDocumentField
	public String absoluteModePathID;
	
	@PojoDocumentListField(itemClass = WebdocsCommandModeTree.class)
	public List<WebdocsCommandModeTree> childCommandModes = new ArrayList<WebdocsCommandModeTree>();

	@SuppressWarnings("rawtypes")
	public static WebdocsCommandModeTree create(String relativeModePath, String absoluteModePathID, CommandFactory commandFactory) {
		WebdocsCommandModeTree cmdModeTree = new WebdocsCommandModeTree();
		cmdModeTree.relativeModePath = relativeModePath;
		cmdModeTree.absoluteModePathID = absoluteModePathID;
		
		List<Class<? extends Command>> registeredCmdClasses = commandFactory.getRegisteredCommandClasses();
		registeredCmdClasses.forEach(cmdClass -> {
			EnterModeCommandClass enterModeAnno = cmdClass.getAnnotation(EnterModeCommandClass.class);
			if(enterModeAnno != null) {
				Class<? extends CommandFactory> childModeCmdFactoryClass = enterModeAnno.commandFactoryClass();
				CommandFactory childModeCmdFactory = CommandFactory.get(childModeCmdFactoryClass);
				String enterModeFirstCmdWord = CommandUsage.cmdWordsForCmdClass(cmdClass)[0];
				String[] modeIDs = CommandUsage.docoptUsagesForCmdClass(cmdClass)[0].split(" ");
				String childModeRelativeModePath = enterModeFirstCmdWord+"/"+
						String.join("/", modeIDs)+"/";
				String childAbsoluteModePathID = cmdModeTree.absoluteModePathID+"_"+enterModeFirstCmdWord;
				WebdocsCommandModeTree childCmdModeTree = create(childModeRelativeModePath, childAbsoluteModePathID, childModeCmdFactory);
				cmdModeTree.childCommandModes.add(childCmdModeTree);
			}
		});
		return cmdModeTree;
	}
	
}
