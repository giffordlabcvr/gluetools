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

import java.util.Arrays;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"multi-copy", "field"}, 
		docoptUsages={"<tableName> (-w <whereClause> | -a) <fromFieldName> <toFieldName> [-b <batchSize>]"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated objects", 
				"-a, --allObjects                               Update all objects",
				"-b <batchSize>, --batchSize <batchSize>        Update batch size" },
		description="Copy values between fields for multiple objects", 
		furtherHelp="Updates to the database are committed in batches, the default batch size is 250.") 
public class MultiCopyFieldCommand extends MultiFieldUpdateCommand {

	public static final String FROM_FIELD_NAME = "fromFieldName";
	public static final String TO_FIELD_NAME = "toFieldName";
	
	private String fromFieldName;
	private String toFieldName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fromFieldName = PluginUtils.configureStringProperty(configElem, FROM_FIELD_NAME, true);
		toFieldName = PluginUtils.configureStringProperty(configElem, TO_FIELD_NAME, true);
	}

	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		Project project = getProjectMode(cmdContext).getProject();
		String tableName = getTableName();
		project.checkModifiableFieldNames(tableName, Arrays.asList(fromFieldName, toFieldName));
		FieldType fromFieldType = project.getModifiableFieldType(tableName, fromFieldName);
		FieldType toFieldType = project.getModifiableFieldType(tableName, toFieldName);
		if(!fromFieldType.equals(toFieldType)) {
			throw new ProjectModeCommandException(Code.INCOMPATIBLE_TYPES_FOR_COPY, fromFieldName, fromFieldType.name(), toFieldName, toFieldType.name());
		}
		return executeUpdates(cmdContext);
	}
	
	@Override
	protected void updateObject(CommandContext cmdContext, GlueDataObject object) {
		Object fromValue = object.readProperty(fromFieldName);
		object.writeProperty(toFieldName, fromValue);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("tableName", new MultiFieldUpdateCommand.TableNameInstantiator());
			registerVariableInstantiator("fromFieldName", new ModifiableFieldInstantiator());
			registerVariableInstantiator("toFieldName", new ModifiableFieldInstantiator());
		}
	}
	
}
