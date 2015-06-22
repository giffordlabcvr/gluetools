package uk.ac.gla.cvr.gluetools.core.command.project.field;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CreateCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datafield.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="create-field")
@CommandClass(description="Create a new data field in this project", 
	docoptUsages={"<name> <type>"}) 
public class CreateFieldCommand extends ProjectModeCommand {

	private String name;
	private FieldType type;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		name = PluginUtils.configureStringProperty(configElem, "name", true);
		type = PluginUtils.configureEnumProperty(FieldType.class, configElem, "type", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getGluetoolsEngine().getCayenneObjectContext();
		Field field = GlueDataObject.create(objContext, Field.class, Field.pkMap(getProjectName(), name));
		field.setProject(getProject(objContext));
		field.setType(type.name());
		objContext.commitChanges();
		return new CreateCommandResult(field.getObjectId());
	}

}
