package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="parameter")
public class EcmaFunctionParameter implements Plugin {

	public static final String NAME = "name";

	private String name;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		name = PluginUtils.configureStringProperty(configElem, NAME, true);
	}

	public String getName() {
		return name;
	}
	
	
	
}
