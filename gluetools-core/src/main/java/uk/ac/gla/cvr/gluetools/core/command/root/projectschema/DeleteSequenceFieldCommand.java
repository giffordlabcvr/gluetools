package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "sequence", "field"}, 
	docoptUsages={"<fieldName>"},
	description="Delete a field from the sequence table") 
public class DeleteSequenceFieldCommand extends ProjectSchemaModeCommand {

	private String fieldName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Field field = GlueDataObject.lookup(objContext, Field.class, Field.pkMap(getProjectName(), fieldName));
		ModelBuilder.deleteSequenceColumnFromModel(cmdContext.peekCommandMode().getServerRuntime(), field.getProject(), field);
		objContext.deleteObject(field);
		return CommandResult.OK;
	}


}
