package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.tablesequences;

import java.util.Map;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "field"}, 
	docoptUsages={"<fieldName>"},
	description="Delete a field from the table") 
public class DeleteSequenceFieldCommand extends TableSequencesModeCommand {

	private String fieldName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Map<String, String> pkMap = Field.pkMap(getProjectName(), fieldName);
		Field field = GlueDataObject.lookup(objContext, Field.class, pkMap);
		ModelBuilder.deleteSequenceColumnFromModel(cmdContext.getGluetoolsEngine().getDbConfiguration(), field.getProject(), field);
		return GlueDataObject.delete(objContext, Field.class, pkMap);
	}


}