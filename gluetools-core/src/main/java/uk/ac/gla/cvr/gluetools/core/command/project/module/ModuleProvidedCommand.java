package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.apache.cayenne.ObjectContext;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;

public abstract class ModuleProvidedCommand<R extends CommandResult, P extends ModulePlugin<P>> extends ModuleModeCommand<R> {

	@SuppressWarnings("unchecked")
	@Override
	public final R execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Module module = GlueDataObject.lookup(objContext, Module.class, Module.pkMap(getModuleName()));
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
		} else {
			return execute(cmdContext, modulePlugin);
		}
	}

	protected abstract R execute(CommandContext cmdContext, P modulePlugin) ;
	
}
