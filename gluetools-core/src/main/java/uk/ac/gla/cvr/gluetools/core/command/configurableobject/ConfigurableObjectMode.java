package uk.ac.gla.cvr.gluetools.core.command.configurableobject;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public interface ConfigurableObjectMode extends InsideProjectMode {

	public String getTableName();
	
	public GlueDataObject getConfigurableObject(CommandContext cmdContext);
	
	public static void registerConfigurableObjectCommands(CommandFactory commandFactory) {
		commandFactory.registerCommandClass(ConfigurableObjectSetFieldCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectUnsetFieldCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectShowPropertyCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectListPropertyCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectSetLinkTargetCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectUnsetLinkTargetCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectAddLinkTargetCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectRemoveLinkTargetCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectClearLinkTargetCommand.class);
		commandFactory.registerCommandClass(ConfigurableObjectListLinkTargetCommand.class);
	}
	
}
