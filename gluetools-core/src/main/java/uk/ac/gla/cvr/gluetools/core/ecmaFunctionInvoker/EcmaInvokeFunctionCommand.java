package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"invoke-function"}, 
		description = "Invoke an ECMAScript function", 
		docoptUsages = { "<functionName> [<argument> ...]" },
		docoptOptions = { },
		metaTags = { },
		furtherHelp = ""
)
public class EcmaInvokeFunctionCommand extends ModulePluginCommand<CommandResult, EcmaFunctionInvoker> {

	public static final String FUNCTION_NAME = "functionName";
	public static final String ARGUMENT = "argument";
	
	
	private String functionName;
	private List<String> arguments;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.functionName = PluginUtils.configureStringProperty(configElem, FUNCTION_NAME, true);
		this.arguments = PluginUtils.configureStringsProperty(configElem, ARGUMENT);
	}

	@Override
	protected CommandResult execute(CommandContext cmdContext, EcmaFunctionInvoker ecmaFunctionInvoker) {
		return ecmaFunctionInvoker.invokeFunction(cmdContext, this.functionName, this.arguments);
	}

	@CompleterClass
	public static class Completer extends ModuleCmdCompleter<EcmaFunctionInvoker> {

		public Completer() {
			super();
			registerVariableInstantiator("functionName", new ModuleVariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						EcmaFunctionInvoker modulePlugin,
						Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return modulePlugin.getFunctions()
							.stream()
							.map(fn -> new CompletionSuggestion(fn.getName(), true))
							.collect(Collectors.toList());
				}
			});
		}
		
	}
	
}
