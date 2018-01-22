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
package uk.ac.gla.cvr.gluetools.core.command.configurableobject;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public interface ConfigurableObjectMode extends InsideProjectMode {

	public String getTableName();
	
	public GlueDataObject getConfigurableObject(CommandContext cmdContext);
	
	public static void registerConfigurableObjectCommands(CommandFactory commandFactory) {
		commandFactory.setCmdGroup(new CommandGroup("configurableObject", "Commands for managing custom object properties", 50, false));
		commandFactory.registerCommandClass(ConfigurableObjectSetFieldCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectUnsetFieldCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectShowPropertyCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectListPropertyCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectSetLinkTargetCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectUnsetLinkTargetCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectAddLinkTargetCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectRemoveLinkTargetCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectClearLinkTargetCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectListLinkTargetCommand.class);
		commandFactory.setCmdGroup(null);
	}
	
}
