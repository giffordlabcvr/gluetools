package uk.ac.gla.cvr.gluetools.core.command.root;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CreateCommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="create-sequence-field")
@CommandClass(description="Create a new sequence field in a project", 
	docoptUsages={"<projectName> <fieldName> <type> [<maxLength>]"}) 
public class CreateSequenceFieldCommand extends RootModeCommand {

	private String projectName;
	private String fieldName;
	private FieldType type;
	private Integer maxLength;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		projectName = PluginUtils.configureStringProperty(configElem, "projectName", true);
		fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", true);
		type = PluginUtils.configureEnumProperty(FieldType.class, configElem, "type", true);
		maxLength = PluginUtils.configureIntProperty(configElem, "maxLength", false);
		if(type == FieldType.VARCHAR && maxLength == null) {
			maxLength = 50;
		}
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Field field = GlueDataObject.create(objContext, Field.class, Field.pkMap(projectName, fieldName));
		Project project = getProject(objContext, projectName);
		field.setProject(project);
		field.setType(type.name());
		field.setMaxLength(maxLength);
		ModelBuilder.addSequenceColumnToModel(cmdContext.peekCommandMode().getServerRuntime(), project, field);
		return new CreateCommandResult(field.getObjectId());
	}

}
