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

import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table.DeleteFieldCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"delete", "custom-table"}, 
		docoptUsages={"<tableName>"},
		description="Delete a custom table") 
public class DeleteCustomTableCommand extends ProjectSchemaModeCommand<DeleteResult> {

	public static final String TABLE_NAME = "tableName";

	private String tableName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		tableName = PluginUtils.configureIdentifierProperty(configElem, TABLE_NAME, true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		String projectName = getProjectName();
		Project project = GlueDataObject.lookup(cmdContext, Project.class, Project.pkMap(projectName));
		CustomTable customTable = GlueDataObject.lookup(cmdContext, CustomTable.class, CustomTable.pkMap(projectName, tableName), true);
		if(customTable != null) {
			{
				Expression exp = ExpressionFactory.matchExp(Field.PROJECT_PROPERTY, projectName);
				exp = exp.andExp(ExpressionFactory.matchExp(Field.TABLE_PROPERTY, tableName));
				List<Field> fields = GlueDataObject.query(cmdContext, Field.class, new SelectQuery(Field.class, exp));
				for(Field field: fields) {
					DeleteFieldCommand.deleteField(cmdContext, field);
				}
			}
			{
				Expression exp = ExpressionFactory.matchExp(Link.PROJECT_PROPERTY, projectName);
				exp = exp.andExp(ExpressionFactory.matchExp(Link.SRC_TABLE_NAME_PROPERTY, tableName)
						.orExp(ExpressionFactory.matchExp(Link.DEST_TABLE_NAME_PROPERTY, tableName)));
				List<Link> links = GlueDataObject.query(cmdContext, Link.class, new SelectQuery(Link.class, exp));
				for(Link link: links) {
					DeleteLinkCommand.deleteLink(cmdContext, link);
				}
			}
			ModelBuilder.deleteCustomTableFromModel(cmdContext.getGluetoolsEngine(), project, customTable);
			GlueDataObject.delete(cmdContext, CustomTable.class, CustomTable.pkMap(projectName, tableName), false);
			cmdContext.commit();
			return new DeleteResult(CustomTable.class, 1);
		} else {
			return new DeleteResult(CustomTable.class, 0);
		}
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("tableName", CustomTable.class, CustomTable.NAME_PROPERTY);
		}
	}


}

