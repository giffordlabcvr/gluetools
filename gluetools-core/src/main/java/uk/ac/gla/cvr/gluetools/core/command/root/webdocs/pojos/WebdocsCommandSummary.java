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
public class WebdocsCommandSummary {

	@PojoDocumentListField(itemClass = String.class)
	public List<String> commandWords = new ArrayList<String>();

	@PojoDocumentField
	public String cmdWordID;

	@PojoDocumentField
	public String docCategory;

	@PojoDocumentField
	public String description;

	@SuppressWarnings("rawtypes")
	public static WebdocsCommandSummary createSummary(Class<? extends Command> cmdClass) {
		WebdocsCommandSummary cmdSummary = new WebdocsCommandSummary();
		CommandUsage cmdUsage = CommandUsage.commandUsageForCmdClass(cmdClass);
		cmdSummary.commandWords.addAll(Arrays.asList(cmdUsage.commandWords()));
		cmdSummary.cmdWordID = cmdUsage.cmdWordID();
		cmdSummary.docCategory = cmdUsage.docCategory();
		cmdSummary.description = cmdUsage.description();
		return cmdSummary;
	}
}
