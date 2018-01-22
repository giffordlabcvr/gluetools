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

import java.util.Collections;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"multi-unset", "field"}, 
		docoptUsages={"<tableName> (-w <whereClause> | -a) <fieldName>  [-b <batchSize>]"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated objects", 
				"-a, --allObjects                               Update all objects in table",
				"-b <batchSize>, --batchSize <batchSize>        Update batch size" },
		description="Unset a field's value for one or more sequences", 
		furtherHelp="The <tableName> argument specifies a configurable object table. "+
				" Possible values are "+ModelBuilder.configurableTablesString+" or a custom table name"+
				". Unsetting means reverting the field value to null."+
				" Updates to the database are committed in batches, the default batch size is 250.") 
public class MultiUnsetFieldCommand extends MultiFieldUpdateCommand {

	public static final String FIELD_NAME = "fieldName";

	private String fieldName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
	}
	
	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		Project project = getProjectMode(cmdContext).getProject();
		project.checkModifiableFieldNames(getTableName(), Collections.singletonList(fieldName));
		return executeUpdates(cmdContext);
	}

	@Override
	protected void updateObject(CommandContext cmdContext, GlueDataObject object) {
		object.writeProperty(fieldName, null);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("tableName", new MultiFieldUpdateCommand.TableNameInstantiator());
			registerVariableInstantiator("fieldName", new ModifiableFieldInstantiator());
		}
		
	}

}
