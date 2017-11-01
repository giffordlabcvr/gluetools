package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptContext;

import jdk.nashorn.api.scripting.JSObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.scripting.NashornContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="ecmaFunctionInvoker")
public class EcmaFunctionInvoker extends ModulePlugin<EcmaFunctionInvoker> {

	public static String SCRIPT_FILE_NAME = "scriptFileName";
	public static String FUNCTION = "function";
	public static String CONFIG_DOCUMENT = "configDocument";
	
	private List<String> scriptFileNames;
	private List<EcmaFunction> functions;
	private Map<String, CommandDocument> configDocuments;
	
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
		this.configDocuments = new LinkedHashMap<String, CommandDocument>();
		List<Element> configDocElements = PluginUtils.findConfigElements(configElem, CONFIG_DOCUMENT);
		for(Element configDocElement: configDocElements) {
			PluginUtils.setValidConfigRecursive(configDocElement);
			String name = configDocElement.getAttribute("name");
			if(name == null || name.length() == 0) {
				throw new PluginConfigException(PluginConfigException.Code.CONFIG_CONSTRAINT_VIOLATION, "Every <configDocument> element must have a name attribute");
			}
			if(!name.matches("^[$A-Za-z_][0-9A-Za-z_$]*$")) {
				throw new PluginConfigException(PluginConfigException.Code.CONFIG_CONSTRAINT_VIOLATION, "Every <configDocument> name attribute must be a valid JavaScript identifier");
			}
			if(configDocuments.containsKey(name)) {
				throw new PluginConfigException(PluginConfigException.Code.CONFIG_CONSTRAINT_VIOLATION, "Every <configDocument> name attribute must be unique");
			}
			Document configDoc = GlueXmlUtils.newDocument();
			configDoc.appendChild(configDoc.importNode(configDocElement, true));
			CommandDocument configCommandDoc = CommandDocumentXmlUtils.xmlDocumentToCommandDocument(configDoc);
			configDocuments.put(name, configCommandDoc);
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
		configDocuments.forEach((name, configDocument) -> {
			nashornContext.setupConfigDocument(scriptContext, name, configDocument);
		});
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


	@Override
	public void loadResources(ConsoleCommandContext consoleCmdContext, File resourceDir, Module module) {
		super.loadResources(consoleCmdContext, resourceDir, module);
		// invalidate script context
		this.scriptContext = null;
	}

	
}
