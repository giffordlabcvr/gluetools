package uk.ac.gla.cvr.gluetools.core.modules;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleExportCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleSetSimplePropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleShowConfigurationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleValidateCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public abstract class ModulePlugin<P extends ModulePlugin<P>> implements Plugin {
	
	private List<String> simplePropertyNames = new ArrayList<String>();
	
	public ModulePlugin() {
		super();
		addModuleDocumentCmdClass(ModuleValidateCommand.class);
		addModuleDocumentCmdClass(ModuleShowConfigurationCommand.class);
		addModuleDocumentCmdClass(ModuleSetSimplePropertyCommand.class);
		addModuleDocumentCmdClass(ModuleExportCommand.class);
	}
	
	protected void addSimplePropertyName(String simplePropertyName) {
		this.simplePropertyNames.add(simplePropertyName);
	}
	
	public List<String> getSimplePropertyNames() {
		return simplePropertyNames;
	}

	@SuppressWarnings("rawtypes")
	private List<Class<? extends Command>> providedCmdClasses = 
			new ArrayList<Class<? extends Command>>();
	
	protected void addModulePluginCmdClass(Class<? extends ModulePluginCommand<?, P>> providedCmdClass) {
		providedCmdClasses.add(providedCmdClass);
	}

	protected void addModuleDocumentCmdClass(Class<? extends ModuleDocumentCommand<?>> providedCmdClass) {
		providedCmdClasses.add(providedCmdClass);
	}

	
	@SuppressWarnings("rawtypes")
	public List<Class<? extends Command>> getProvidedCommandClasses() {
		return providedCmdClasses;
	}
	
	protected Project getProject(CommandContext cmdContext) {
		return ((ProjectMode) cmdContext.peekCommandMode()).getProject();
	}

}
