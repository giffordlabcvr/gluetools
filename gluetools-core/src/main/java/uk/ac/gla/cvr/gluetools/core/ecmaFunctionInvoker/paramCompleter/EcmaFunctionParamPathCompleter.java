package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.paramCompleter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName = "pathCompleter")
public class EcmaFunctionParamPathCompleter extends EcmaFunctionParamCompleter {

	private boolean directoriesOnly;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.directoriesOnly = 
				Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, 
						"directoriesOnly", false)).orElse(false);
	}



	@SuppressWarnings("rawtypes")
	@Override
	public List<CompletionSuggestion> instantiate(
			ConsoleCommandContext cmdContext,
			Class<? extends Command> cmdClass, Map<String, Object> bindings,
			String prefix) {
		return AdvancedCmdCompleter.completePath(cmdContext, prefix, directoriesOnly);
	}

}
