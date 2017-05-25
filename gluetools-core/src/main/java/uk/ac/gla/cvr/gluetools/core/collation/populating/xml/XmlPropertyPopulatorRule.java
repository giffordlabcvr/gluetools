package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="propertyPopulator")
public class XmlPropertyPopulatorRule extends BaseXmlPropertyPopulatorRule {
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
		super.configure(pluginConfigContext, configElem);
		populateProperty(pluginConfigContext, configElem);
		setValueConverters(PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
				PluginUtils.findConfigElements(configElem, "valueConverter")));
		setMainExtractor(PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, configElem));
	}

	protected void populateProperty(PluginConfigContext pluginConfigContext, Element configElem) {
		setProperty(PluginUtils.configureString(configElem, "@property", true));
	}
	
}
