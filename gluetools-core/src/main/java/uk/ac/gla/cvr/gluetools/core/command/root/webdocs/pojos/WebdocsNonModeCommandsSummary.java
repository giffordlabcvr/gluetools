package uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandFactory;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class WebdocsNonModeCommandsSummary {


	@PojoDocumentListField(itemClass = WebdocsCommandCategory.class)
	public List<WebdocsCommandCategory> commandCategories = new ArrayList<WebdocsCommandCategory>();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static WebdocsNonModeCommandsSummary create() {
		WebdocsNonModeCommandsSummary nonModeCommandsSummary = new WebdocsNonModeCommandsSummary();
		
		CommandFactory commandFactory = CommandFactory.get(RootCommandFactory.class);

		commandFactory.getCommandGroupRegistry().getCmdGroupToCmdClasses().forEach((cmdGroup, setOfClasses) -> {
			if(!cmdGroup.isNonModeSpecific()) {
				return;
			}
			List<WebdocsCommandSummary> commandSummaries = new ArrayList<WebdocsCommandSummary>();
			setOfClasses.forEach(cmdClass -> {
				commandSummaries.add(WebdocsCommandSummary.createSummary((Class<? extends Command>) cmdClass));
			});
			nonModeCommandsSummary.commandCategories.add(WebdocsCommandCategory.create(cmdGroup.getDescription(), commandSummaries));
		});
		
		return nonModeCommandsSummary;
	}
	
}
