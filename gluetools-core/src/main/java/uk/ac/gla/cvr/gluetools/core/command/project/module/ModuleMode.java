package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.ModuleCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;

@CommandModeClass(commandFactoryClass = ModuleModeCommandFactory.class)
public class ModuleMode extends CommandMode<ModuleCommand> {

	
	private String moduleName;
	
	// dynamic command factory
	public ModuleMode(CommandContext cmdContext, ModuleCommand command, String moduleName) {
		super(new ModuleModeCommandFactory(cmdContext, moduleName), command, moduleName);
		this.moduleName = moduleName;
	}

	
	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(ModuleModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "moduleName", moduleName);
		}
	}

	public String getModuleName() {
		return moduleName;
	}

	
}
