package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class WebdocsCommandDocumentation {

	@PojoDocumentListField(itemClass = String.class)
	public List<String> commandWords = new ArrayList<String>();

	// could be a more complex object based on the docopt FSM in future.
	@PojoDocumentListField(itemClass = String.class)
	public List<String> usagePatterns = new ArrayList<String>();

	// could be a more complex object
	@PojoDocumentListField(itemClass = String.class)
	public List<String> optionDocs;

	@PojoDocumentField
	public String description;

	@PojoDocumentField
	public String furtherHelp;

	@SuppressWarnings("rawtypes")
	public static WebdocsCommandDocumentation createDocumentation(Class<? extends Command> cmdClass) {
		WebdocsCommandDocumentation cmdDoc = new WebdocsCommandDocumentation();
		CommandUsage cmdUsage = CommandUsage.commandUsageForCmdClass(cmdClass);
		cmdDoc.commandWords.addAll(Arrays.asList(cmdUsage.commandWords()));
		cmdDoc.usagePatterns.addAll(Arrays.asList(cmdUsage.docoptUsages()));
		cmdDoc.optionDocs.addAll(Arrays.asList(cmdUsage.docoptOptions()));
		cmdDoc.description = cmdUsage.description();
		cmdDoc.furtherHelp = cmdUsage.furtherHelp();
		return cmdDoc;
	}
}