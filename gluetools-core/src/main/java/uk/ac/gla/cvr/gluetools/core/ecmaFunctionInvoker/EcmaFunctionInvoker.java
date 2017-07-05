package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import java.util.List;

import javax.script.ScriptContext;

import jdk.nashorn.api.scripting.JSObject;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.scripting.NashornContext;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="ecmaFunctionInvoker")
public class EcmaFunctionInvoker extends ModulePlugin<EcmaFunctionInvoker> {

	public static String SCRIPT_FILE_NAME = "scriptFileName";
	public static String FUNCTION = "function";
	
	private List<String> scriptFileNames;
	private List<EcmaFunction> functions;
	
	// scripts will be loaded into this context once only during the lifetime
	// of this EcmaFunctionInvoker 
	private ScriptContext scriptContext; 

	
	public EcmaFunctionInvoker() {
		super();
		addModulePluginCmdClass(EcmaInvokeFunctionCommand.class);
	}


	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.scriptFileNames = PluginUtils.configureStringsProperty(configElem, SCRIPT_FILE_NAME, 1, null);
		functions = PluginFactory.createPlugins(pluginConfigContext, EcmaFunction.class, 
				PluginUtils.findConfigElements(configElem, FUNCTION));
		for(String scriptFileName: scriptFileNames) {
			registerResourceName(scriptFileName);
		}
	}
	

	public ScriptContext ensureScriptContext(CommandContext cmdContext) {
		if(scriptContext != null) {
			return scriptContext;
		}
		NashornContext nashornContext = cmdContext.getNashornContext();
		scriptContext = nashornContext.newScriptContext();
		for(String scriptFileName: scriptFileNames) {
			byte[] scriptSourceBytes = getResource(cmdContext, scriptFileName);
			String scriptSource = new String(scriptSourceBytes);
			nashornContext.loadScriptInContext(scriptContext, scriptFileName, scriptSource);
		}
		return scriptContext;
	}


	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		NashornContext nashornContext = cmdContext.getNashornContext();
		nashornContext.setScriptContext(ensureScriptContext(cmdContext));
		for(EcmaFunction function: getFunctions()) {
			JSObject fnObj = nashornContext.lookupFunction(function.getName());
			if(fnObj == null) {
				throw new EcmaFunctionInvokerException(EcmaFunctionInvokerException.Code.FUNCTION_LOOKUP_EXCEPTION, 
						getModuleName(), function.getName(), "No such function found in resources "+scriptFileNames);
			}
		}
		
		
		
	}

	public List<EcmaFunction> getFunctions() {
		return functions;
	}

	public CommandResult invokeFunction(CommandContext cmdContext, String functionName, List<String> arguments) {
		return getFunctions()
				.stream()
				.filter(fn -> fn.getName().equals(functionName))
				.findFirst()
				.orElseThrow(() -> 
					new EcmaFunctionInvokerException(EcmaFunctionInvokerException.Code.FUNCTION_NAME_UNKNOWN, 
					functionName, this.getModuleName()))
				.invoke(cmdContext, this, arguments);
	}

	
}
