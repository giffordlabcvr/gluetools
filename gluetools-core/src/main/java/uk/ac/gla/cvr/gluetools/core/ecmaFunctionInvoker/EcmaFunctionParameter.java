package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.paramCompleter.EcmaFunctionParamCompleter;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.paramCompleter.EcmaFunctionParamCompleterFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="parameter")
public class EcmaFunctionParameter implements Plugin {

	public static final String NAME = "name";

	private String name;
	private EcmaFunctionParamCompleter paramCompleter = null;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		name = PluginUtils.configureStringProperty(configElem, NAME, true);
		EcmaFunctionParamCompleterFactory paramCompleterFactory = PluginFactory.get(EcmaFunctionParamCompleterFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(paramCompleterFactory.getElementNames());
		Element paramCompleterElem = PluginUtils.findConfigElement(configElem, alternateElemsXPath, false);
		if(paramCompleterElem != null) {
			paramCompleter = paramCompleterFactory.createFromElement(pluginConfigContext, paramCompleterElem);
		}
	}

	public String getName() {
		return name;
	}

	public EcmaFunctionParamCompleter getParamCompleter() {
		return paramCompleter;
	}
	
}
