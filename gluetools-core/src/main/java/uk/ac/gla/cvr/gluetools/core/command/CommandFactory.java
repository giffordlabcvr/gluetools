package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.HelpCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.QuitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.SetDirectoryCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;

public abstract class CommandFactory extends PluginFactory<Command> {

	protected CommandFactory() {
		super();
		registerPluginClass(QuitCommand.class);
		registerPluginClass(ExitCommand.class);
		registerPluginClass(HelpCommand.class);
		registerPluginClass(SetDirectoryCommand.class);
	}
	
	@Override
	protected void registerPluginClass(Class<? extends Command> commandClass) {
		CommandClass cmdClassAnno = commandClass.getAnnotation(CommandClass.class);
		if(cmdClassAnno == null) { throw new RuntimeException("No CommandClass annotation for "+commandClass.getCanonicalName()); }
		super.registerPluginClass(commandClass);
	}

	
	
}
