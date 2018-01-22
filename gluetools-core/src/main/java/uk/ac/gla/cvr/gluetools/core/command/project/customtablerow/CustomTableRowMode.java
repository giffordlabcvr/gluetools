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
package uk.ac.gla.cvr.gluetools.core.command.project.customtablerow;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.CustomTableRowCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.RenderableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = CustomTableRowModeCommandFactory.class)
public class CustomTableRowMode extends CommandMode<CustomTableRowCommand> implements ConfigurableObjectMode, RenderableObjectMode {

	
	private CustomTable customTable;
	private String rowId;
	private Project project;
	
	public CustomTableRowMode(Project project, CustomTableRowCommand command, CustomTable customTable, String rowId) {
		super(command, customTable.getName(), rowId);
		this.project = project;
		this.customTable = customTable;
		this.rowId = rowId;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(CustomTableRowModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "tableName", customTable.getName());
			appendModeConfigToElem(elem, "rowId", rowId);
		}
	}

	public CustomTable getCustomTable() {
		return customTable;
	}

	public String getRowId() {
		return rowId;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public String getTableName() {
		return getCustomTable().getName();
	}

	@Override
	public GlueDataObject getConfigurableObject(CommandContext cmdContext) {
		return lookupCustomTableRow(cmdContext);
	}

	protected CustomTableObject lookupCustomTableRow(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, customTable.getRowClass(), CustomTableObject.pkMap(rowId));
	}

	@Override
	public GlueDataObject getRenderableObject(CommandContext cmdContext) {
		return lookupCustomTableRow(cmdContext);
	}

	
}
