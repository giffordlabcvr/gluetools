package uk.ac.gla.cvr.gluetools.core.command.project.module;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;

public abstract class ModulePluginCommand<R extends CommandResult, P extends ModulePlugin<P>> extends ModuleModeCommand<R> {

	@SuppressWarnings("unchecked")
	@Override
	public final R execute(CommandContext cmdContext) {
		
		Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(getModuleName()));
		@SuppressWarnings("unchecked")
		P modulePlugin = (P) module.getModulePlugin(cmdContext.getGluetoolsEngine());
		if(this instanceof ProvidedProjectModeCommand) {
			CommandMode<?> moduleMode = cmdContext.popCommandMode();
			// run the command in the next mode up (project mode)
			try {
				return execute(cmdContext, modulePlugin);
			} finally {
				cmdContext.pushCommandMode(moduleMode);
			}
		}
		if(this instanceof ProvidedSchemaProjectModeCommand) {
			CommandMode<?> moduleMode = cmdContext.popCommandMode();
			ProjectMode projectMode = (ProjectMode) cmdContext.popCommandMode();
			String projectName = projectMode.getProject().getName();
			// run the command in schema-project mode
			try(ModeCloser modeCloser = cmdContext.pushCommandMode("schema-project", projectName)) {
				return execute(cmdContext, modulePlugin);
			} finally {
				cmdContext.pushCommandMode(projectMode);
				cmdContext.pushCommandMode(moduleMode);
			}
		} 
		return execute(cmdContext, modulePlugin);
	}

	protected abstract R execute(CommandContext cmdContext, P modulePlugin) ;
	
}
