package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.paramCompleter;

import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public abstract class EcmaFunctionParamCompleter implements Plugin {

	@SuppressWarnings("rawtypes")
	public abstract List<CompletionSuggestion> instantiate(
			ConsoleCommandContext cmdContext,
			Class<? extends Command> cmdClass, Map<String, Object> bindings,
			String prefix);
}
