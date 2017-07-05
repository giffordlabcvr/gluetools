package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import java.util.List;

import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.NashornException;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.scripting.NashornContext;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvokerException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="function")
public class EcmaFunction implements Plugin {

	public static final String NAME = "name";
	public static final String PARAMETER = "parameter";

	private String name;
	private List<EcmaFunctionParameter> parameters;
	private EcmaFunctionResultType<?> resultType;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		
		EcmaFunctionResultTypeFactory resultTypeFactory = PluginFactory.get(EcmaFunctionResultTypeFactory.creator);

		name = PluginUtils.configureStringProperty(configElem, NAME, true);
		parameters = PluginFactory.createPlugins(pluginConfigContext, EcmaFunctionParameter.class, 
				PluginUtils.findConfigElements(configElem, PARAMETER));
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(resultTypeFactory.getElementNames());
		Element resultTypeElem = PluginUtils.findConfigElement(configElem, alternateElemsXPath, false);
		if(resultTypeElem != null) {
			resultType = resultTypeFactory.createFromElement(pluginConfigContext, resultTypeElem);
		} else {
			resultType = new EcmaFunctionDocumentResultType();
		}
	}

	public String getName() {
		return name;
	}

	public List<EcmaFunctionParameter> getParameters() {
		return parameters;
	}

	public CommandResult invoke(CommandContext cmdContext, EcmaFunctionInvoker ecmaFunctionInvoker, List<String> arguments) {
		if(arguments.size() != getParameters().size()) {
			throw new EcmaFunctionInvokerException(
					EcmaFunctionInvokerException.Code.INCORRECT_NUMBER_OF_ARGUMENTS, 
						ecmaFunctionInvoker.getModuleName(), getName(), 
						Integer.toString(getParameters().size()), Integer.toString(arguments.size()));
		}
		NashornContext nashornContext = cmdContext.getNashornContext();
		nashornContext.setScriptContext(ecmaFunctionInvoker.ensureScriptContext(cmdContext));
		JSObject functionJSObject = nashornContext.lookupFunction(getName());
		Object cmdResultObj = null;
		try {
			cmdResultObj = nashornContext.invokeFunction(functionJSObject, arguments.toArray(new Object[]{}));
		} catch(NashornException ne) {
			nashornContext.recastException(ne.getFileName(), ne);
		}
		if(cmdResultObj == null) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_INVOCATION_EXCEPTION, ecmaFunctionInvoker.getModuleName(), getName(), 
					"Result was null");
		}
		return resultType.glueResultFromReturnObject(ecmaFunctionInvoker, getName(), cmdResultObj);
	}
	
	
	
}
