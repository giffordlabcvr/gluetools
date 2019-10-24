/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.NashornException;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.scripting.CommandDocumentToMapVisitor;
import uk.ac.gla.cvr.gluetools.core.command.scripting.NashornContext;
import uk.ac.gla.cvr.gluetools.core.command.scripting.NashornScriptingException;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvokerException.Code;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.resultType.EcmaFunctionOkFromNullResultType;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.resultType.EcmaFunctionResultType;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.resultType.EcmaFunctionResultTypeFactory;
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
	public static final String CONSUMES_BINARY = "consumesBinary";
	public static final String CONSUMES_DOCUMENT = "consumesDocument";

	private String name;
	private Boolean consumesBinary;
	private Boolean consumesDocument;
	private List<EcmaFunctionParameter> parameters;
	private EcmaFunctionResultType<?> resultType;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		
		EcmaFunctionResultTypeFactory resultTypeFactory = PluginFactory.get(EcmaFunctionResultTypeFactory.creator);

		name = PluginUtils.configureStringProperty(configElem, NAME, true);
		consumesBinary = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, CONSUMES_BINARY, false)).orElse(false);
		consumesDocument = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, CONSUMES_DOCUMENT, false)).orElse(false);
		parameters = PluginFactory.createPlugins(pluginConfigContext, EcmaFunctionParameter.class, 
				PluginUtils.findConfigElements(configElem, PARAMETER));
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(resultTypeFactory.getElementNames());
		Element resultTypeElem = PluginUtils.findConfigElement(configElem, alternateElemsXPath, false);
		if(resultTypeElem != null) {
			resultType = resultTypeFactory.createFromElement(pluginConfigContext, resultTypeElem);
		} else {
			resultType = new EcmaFunctionOkFromNullResultType();
		}
	}

	public String getName() {
		return name;
	}

	protected Boolean consumesBinary() {
		return consumesBinary;
	}

	protected Boolean consumesDocument() {
		return consumesDocument;
	}
	
	public List<EcmaFunctionParameter> getParameters() {
		return parameters;
	}

	public CommandResult invoke(CommandContext cmdContext, EcmaFunctionInvoker ecmaFunctionInvoker, List<String> arguments,
			CommandDocument document) {
		if(consumesDocument && consumesBinary) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_INVOCATION_EXCEPTION, ecmaFunctionInvoker.getModuleName(), getName(), 
					"EcmaFunction may not have both consumesBinary and consumesDocument set to true.");
		}
		if(consumesBinary) {
			if(getParameters().size() == 0 || !getParameters().get(0).getName().equals("base64")) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_INVOCATION_EXCEPTION, ecmaFunctionInvoker.getModuleName(), getName(), 
						"EcmaFunction with consumesBinary set to true must define first parameter named 'base64'.");
			}
		}
		if(consumesDocument) {
			if(getParameters().size() != 1 || !getParameters().get(0).getName().equals("document")) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_INVOCATION_EXCEPTION, ecmaFunctionInvoker.getModuleName(), getName(), 
						"EcmaFunction with consumesDocument set to true must define single parameter named 'document'.");
			}
			if(arguments.size() > 0) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_INVOCATION_EXCEPTION, ecmaFunctionInvoker.getModuleName(), getName(), 
						"EcmaFunction with consumesDocument set to true cannot be invoked with arguments.");
			}
		} else {
			if(document != null) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_INVOCATION_EXCEPTION, ecmaFunctionInvoker.getModuleName(), getName(), 
						"EcmaFunction with consumesDocument set to false cannot be invoked with a document.");
			}
		}
		if(arguments.size() != getParameters().size() && !consumesDocument) {
			throw new EcmaFunctionInvokerException(
					EcmaFunctionInvokerException.Code.INCORRECT_NUMBER_OF_ARGUMENTS, 
						ecmaFunctionInvoker.getModuleName(), getName(), 
						Integer.toString(getParameters().size()), Integer.toString(arguments.size()));
		}
		NashornContext nashornContext = cmdContext.getNashornContext();
		nashornContext.setScriptContext(ecmaFunctionInvoker.ensureScriptContext(cmdContext));
		JSObject functionJSObject = nashornContext.lookupFunction(getName());
		if(functionJSObject == null) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_INVOCATION_EXCEPTION, ecmaFunctionInvoker.getModuleName(), getName(), "Function not found: "+getName());
		}
		Object cmdResultObj = null;
		Object[] inputArray;
		if(consumesDocument) {
			CommandDocumentToMapVisitor visitor = new CommandDocumentToMapVisitor();
			document.accept(visitor);
			Map<String, Object> rootMap = visitor.getRootMap();
			JSObject objToNativeFunction = nashornContext.lookupFunction("objToNative");
			Object nativeObj = nashornContext.invokeFunction(objToNativeFunction, new Object[]{rootMap});
			inputArray = new Object[]{nativeObj};
		} else {
			inputArray = arguments.toArray(new Object[]{});
		}
		try {
			cmdResultObj = nashornContext.invokeFunction(functionJSObject, inputArray);
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
