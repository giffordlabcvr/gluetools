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
package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create", "field"}, 
	docoptUsages={"<fieldName> <type> [<maxLength>]"},
	description="Create a new field in the table",
	metaTags = {},
	furtherHelp="The field name must be a valid database identifier, e.g. my_field_1") 
public class CreateFieldCommand extends TableModeCommand<CreateResult> {

	public static final String MAX_LENGTH = "maxLength";
	public static final String TYPE = "type";
	public static final String FIELD_NAME = "fieldName";

	private String fieldName;
	private FieldType type;
	private Integer maxLength;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureIdentifierProperty(configElem, FIELD_NAME, true);
		type = PluginUtils.configureEnumProperty(FieldType.class, configElem, TYPE, true);
		maxLength = PluginUtils.configureIntProperty(configElem, MAX_LENGTH, false);
		if(type == FieldType.VARCHAR) {
			if(maxLength == null) {
				maxLength = 50;
			} else {
				if(type == FieldType.VARCHAR && maxLength > 10000) {
					throw new CommandException(Code.COMMAND_USAGE_ERROR, "The <maxLength> upper limit for VARCHAR is 10000");
				}
			}
		} else {
			if(maxLength != null) {
				throw new CommandException(Code.COMMAND_USAGE_ERROR, "A <maxLength> may only be specified for VARCHAR");
			}
		}
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		Field field = GlueDataObject.create(cmdContext, Field.class, Field.pkMap(getProjectName(), getTableName(), fieldName), false);
		Project project = GlueDataObject.lookup(cmdContext, Project.class, Project.pkMap(getProjectName()));
		field.setType(type.name());
		field.setTable(getTableName());
		field.setMaxLength(maxLength);
		ModelBuilder.addFieldToModel(cmdContext.getGluetoolsEngine(), project, field);
		field.setProject(project);
		cmdContext.commit();
		return new CreateResult(Field.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerEnumLookup("type", FieldType.class);
		}
	}
	
	
}
