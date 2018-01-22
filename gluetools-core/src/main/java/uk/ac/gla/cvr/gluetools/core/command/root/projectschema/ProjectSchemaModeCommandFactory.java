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
package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ProjectSchemaModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<ProjectSchemaModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ProjectSchemaModeCommandFactory.class, ProjectSchemaModeCommandFactory::new);

	private ProjectSchemaModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(TableCommand.class);
		registerCommandClass(ExitCommand.class);

		setCmdGroup(new CommandGroup("customTables", "Commands for managing custom tables", 25, false));
		registerCommandClass(CreateCustomTableCommand.class);
		registerCommandClass(DeleteCustomTableCommand.class);
		registerCommandClass(ListCustomTableCommand.class);

		setCmdGroup(new CommandGroup("customLinks", "Commands for managing custom relational links", 26, false));
		registerCommandClass(CreateLinkCommand.class);
		registerCommandClass(DeleteLinkCommand.class);
		registerCommandClass(ListLinkCommand.class);

	}
	

}
