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
package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","custom-table-row"}, 
	docoptUsages={"[-a] [-C] <tableName> <rowId>"},
	docoptOptions={"-a, --allowExisting  Allow case where row ID <rowID> exists",
			"-C, --noCommit     Don't commit to the database [default: false]"},
	metaTags={CmdMeta.updatesDatabase},
	description="Create a new row in a custom table") 
public class CreateCustomTableRowCommand extends ProjectModeCommand<CreateResult> {

	public static final String TABLE_NAME = "tableName";
	public static final String ROW_ID = "rowId";
	public static final String ALLOW_EXISTING = "allowExisting";
	public static final String NO_COMMIT = "noCommit";
	
	private String tableName;
	private String rowId;
	private boolean allowExisting;
	private boolean noCommit;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		tableName = PluginUtils.configureStringProperty(configElem, TABLE_NAME, true);
		rowId = PluginUtils.configureStringProperty(configElem, ROW_ID, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
		allowExisting = PluginUtils.configureBooleanProperty(configElem, ALLOW_EXISTING, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		Project project = getProjectMode(cmdContext).getProject();
		project.checkCustomTableName(tableName);
		CustomTable customTable = project.getCustomTable(tableName);
		Class<? extends CustomTableObject> rowClass = customTable.getRowClass();
		Map<String, String> pkMap = CustomTableObject.pkMap(rowId);
		CustomTableObject existing = null;
		if(allowExisting) {
			existing = GlueDataObject.lookup(cmdContext, rowClass, pkMap, true);
		}
		int numCreated = 0;
		if(existing == null) {
			GlueDataObject.create(cmdContext, rowClass, pkMap, false);
			numCreated = 1;
		}
		if(!noCommit) {
			cmdContext.commit();
		}
		return new CreateResult(rowClass, numCreated);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("tableName", new AdvancedCmdCompleter.CustomTableNameInstantiator());
		}
	}
	
}
