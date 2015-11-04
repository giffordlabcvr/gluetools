package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.ModuleCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = ModuleModeCommandFactory.class)
public class ModuleMode extends CommandMode<ModuleCommand> implements InsideProjectMode {

	
	private String moduleName;
	private Project project;
	
	// dynamic command factory
	public ModuleMode(CommandContext cmdContext, Project project, ModuleCommand command, String moduleName) {
		super(new ModuleModeCommandFactory(cmdContext, moduleName), command, moduleName);
		this.project = project;
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


	@Override
	public Project getProject() {
		return project;
	}

	
}
