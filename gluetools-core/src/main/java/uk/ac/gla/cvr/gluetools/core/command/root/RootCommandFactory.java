package uk.ac.gla.cvr.gluetools.core.command.root;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.WebdocsDocumentCommandCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.WebdocsDocumentCommandModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.WebdocsDocumentModuleCommandCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.WebdocsDocumentModuleTypeCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.WebdocsListCommandModesCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.WebdocsListModuleTypesCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class RootCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<RootCommandFactory> creator = new
			Multiton.SuppliedCreator<>(RootCommandFactory.class, RootCommandFactory::new);

	private RootCommandFactory() {
	}	

	protected void populateCommandTree() {
		super.populateCommandTree();
		
		registerCommandClass(ProjectCommand.class);
		registerCommandClass(CreateProjectCommand.class);
		registerCommandClass(DeleteProjectCommand.class);
		registerCommandClass(ListProjectCommand.class);
		registerCommandClass(ProjectSchemaCommand.class);
		registerCommandClass(ExitCommand.class);
		
		// commands for web-based reference documentation
		registerCommandClass(WebdocsListModuleTypesCommand.class);
		registerCommandClass(WebdocsDocumentModuleTypeCommand.class);
		registerCommandClass(WebdocsDocumentModuleCommandCommand.class);
		registerCommandClass(WebdocsListCommandModesCommand.class);
		registerCommandClass(WebdocsDocumentCommandModeCommand.class);
		registerCommandClass(WebdocsDocumentCommandCommand.class);
	}
}
