package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.tablesequences;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
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
	furtherHelp="The field name must be a valid database identifier, e.g. MY_FIELD_1") 
public class CreateSequenceFieldCommand extends TableSequencesModeCommand {

	private String fieldName;
	private FieldType type;
	private Integer maxLength;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureIdentifierProperty(configElem, "fieldName", true);
		type = PluginUtils.configureEnumProperty(FieldType.class, configElem, "type", true);
		maxLength = PluginUtils.configureIntProperty(configElem, "maxLength", false);
		if(type == FieldType.VARCHAR && maxLength == null) {
			maxLength = 50;
		}
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Field field = GlueDataObject.create(objContext, Field.class, Field.pkMap(getProjectName(), fieldName), false);
		Project project = GlueDataObject.lookup(objContext, Project.class, Project.pkMap(getProjectName()));
		field.setProject(project);
		field.setType(type.name());
		field.setMaxLength(maxLength);
		ModelBuilder.addSequenceColumnToModel(cmdContext.getGluetoolsEngine().getDbConfiguration(), project, field);
		cmdContext.commit();
		return new CreateResult(Field.class, 1);
	}

}
