package uk.ac.gla.cvr.gluetools.core.command.project.module;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter.VariableInstantiator;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
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
	
	public static class ModuleCmdCompleter<P extends ModulePlugin<P>> extends AdvancedCmdCompleter {
		
		
		protected abstract class ModuleVariableInstantiator extends VariableInstantiator {

			@Override
			@SuppressWarnings({ "rawtypes", "unchecked" })
			public final List<CompletionSuggestion> instantiate(
					ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
					Map<String, Object> bindings, String prefix) {
				ModuleMode moduleMode = (ModuleMode) cmdContext.peekCommandMode();
				String moduleName = moduleMode.getModuleName();
				Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(moduleName));
				P modulePlugin;
				try {
					modulePlugin = (P) module.getModulePlugin(cmdContext.getGluetoolsEngine());
				} catch(Exception e) {
					return Collections.emptyList();
				}
				return instantiate(cmdContext, modulePlugin, cmdClass, bindings, prefix);
			}

		
			@SuppressWarnings({ "rawtypes" })
			protected abstract List<CompletionSuggestion> instantiate(
					ConsoleCommandContext cmdContext, P modulePlugin, Class<? extends Command> cmdClass,
					Map<String, Object> bindings, String prefix); 

		}

		
	}
	
}
