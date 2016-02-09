package uk.ac.gla.cvr.gluetools.core.command.project.module;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;

public class ModuleModeCommandFactory extends BaseCommandFactory {

	@SuppressWarnings("rawtypes")
	private List<Class<? extends Command>> cmdClasses = null;
	private String moduleName;
	
	public ModuleModeCommandFactory(CommandContext cmdContext, String moduleName) {
		super();
		this.moduleName = moduleName;
		refreshCommandTree(cmdContext);
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void refreshCommandTree(CommandContext cmdContext) {
		Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(moduleName));
		@SuppressWarnings("rawtypes")
		List<Class<? extends Command>> providedCmdClasses = module.getProvidedCommandClasses(cmdContext.getGluetoolsEngine());
		if(cmdClasses == null || !providedCmdClasses.equals(cmdClasses)) {
			cmdClasses = providedCmdClasses;
			resetCommandTree();
			populateCommandTree();
			cmdClasses.forEach(cmdClass -> 
				registerCommandClass((Class<? extends Command>) cmdClass)	
			);
			registerCommandClass(ExitCommand.class);
		}
	}
	

}
