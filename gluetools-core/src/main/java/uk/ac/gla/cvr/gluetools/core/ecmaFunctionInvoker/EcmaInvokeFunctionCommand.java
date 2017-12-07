package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
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
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.paramCompleter.EcmaFunctionParamCompleter;
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
public class EcmaInvokeFunctionCommand extends ModulePluginCommand<CommandResult, EcmaFunctionInvoker> implements ProvidedProjectModeCommand {

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

		@SuppressWarnings("rawtypes")
		public Completer() {
			super();
			registerVariableInstantiator("functionName", new ModuleVariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						EcmaFunctionInvoker ecmaFunctionInvoker,
						Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return ecmaFunctionInvoker.getFunctions()
							.stream()
							.map(fn -> new CompletionSuggestion(fn.getName(), true))
							.collect(Collectors.toList());
				}
			});
			registerVariableInstantiator("argument", new ModuleVariableInstantiator() {

				@SuppressWarnings("unchecked")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						EcmaFunctionInvoker ecmaFunctionInvoker,
						Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					String functionName = (String) bindings.get("functionName");
					if(functionName == null) {
						return null;
					}
					EcmaFunction function = ecmaFunctionInvoker.getFunction(functionName);
					if(function == null) {
						return null;
					}
					List<EcmaFunctionParameter> parameters = function.getParameters();
					Object bindingObj = bindings.get("argument");
					List<String> argBindings;
					if(bindingObj instanceof List) {
						argBindings = (List<String>) bindingObj;
					} else if(bindingObj instanceof String) {
						argBindings = Arrays.asList((String) bindingObj);
					} else if(bindingObj == null) {
						argBindings = new ArrayList<String>();
					} else {
						return null;
					}
					if(argBindings == null || argBindings.size() >= parameters.size()) {
						return null;
					}
					EcmaFunctionParameter thisParam = parameters.get(argBindings.size());
					EcmaFunctionParamCompleter paramCompleter = thisParam.getParamCompleter();
					if(paramCompleter != null) {
						Map<String, Object> paramCompleterBindings = new LinkedHashMap<String, Object>();
						for(int i = 0; i < argBindings.size(); i++) {
							paramCompleterBindings.put(parameters.get(i).getName(), argBindings.get(i));
						}
						List<CompletionSuggestion> paramCompleterResult = paramCompleter.instantiate(cmdContext, cmdClass, paramCompleterBindings, prefix);
						if(paramCompleterResult != null && paramCompleterResult.size() > 0) {
							return paramCompleterResult;
						}
					}
					return Arrays.asList(
							new CompletionSuggestion("<"+thisParam.getName()+">", true));
				}
			});
		}
		
	}
	
}
