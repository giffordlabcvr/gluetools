/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.root;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.WebdocsDocumentModeCommandCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.WebdocsDocumentCommandModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.WebdocsDocumentModuleCommandCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.WebdocsDocumentModuleTypeCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.WebdocsDocumentNonModeCommandCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.WebdocsDocumentNonModeCommandsCommand;
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
		
		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(ProjectCommand.class);
		registerCommandClass(ProjectSchemaCommand.class);
		registerCommandClass(ExitCommand.class);

		setCmdGroup(new CommandGroup("projects", "Commands for managing projects", 50, false));
		registerCommandClass(CreateProjectCommand.class);
		registerCommandClass(DeleteProjectCommand.class);
		registerCommandClass(ListProjectCommand.class);
		
		// commands for web-based reference documentation
		registerCommandClass(WebdocsListModuleTypesCommand.class);
		registerCommandClass(WebdocsDocumentModuleTypeCommand.class);
		registerCommandClass(WebdocsDocumentModuleCommandCommand.class);
		registerCommandClass(WebdocsListCommandModesCommand.class);
		registerCommandClass(WebdocsDocumentCommandModeCommand.class);
		registerCommandClass(WebdocsDocumentModeCommandCommand.class);
		registerCommandClass(WebdocsDocumentNonModeCommandsCommand.class);
		registerCommandClass(WebdocsDocumentNonModeCommandCommand.class);
	}
}
