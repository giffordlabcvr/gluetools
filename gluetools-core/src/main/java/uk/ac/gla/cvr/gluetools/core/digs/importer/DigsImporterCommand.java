package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter.VariableInstantiator;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;

public abstract class DigsImporterCommand<R extends CommandResult> extends ModulePluginCommand<R, DigsImporter> 
	implements ProvidedProjectModeCommand {

	protected static class DigsDbNameInstantiator extends VariableInstantiator {
		@SuppressWarnings("rawtypes")
		@Override
		protected List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			try {
				return DigsImporter.listDigsDatabases(cmdContext).stream()
						.map(dbName -> new CompletionSuggestion(dbName, true))
						.collect(Collectors.toList());
			} catch(Exception e) {
				return null;
			}
		}
		
	}
	
}
