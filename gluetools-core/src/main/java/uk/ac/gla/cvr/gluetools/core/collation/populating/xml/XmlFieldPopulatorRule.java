package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="fieldPopulator", deprecated=true,
		deprecationWarning="Plugin element \"fieldPopulator\" with attribute \"fieldName\" "
		+"is deprecated, please use element \"propertyPopulator\" with attribute \"property\"")
public class XmlFieldPopulatorRule extends XmlPropertyPopulatorRule {
	
	protected void populateProperty(PluginConfigContext pluginConfigContext, Element configElem) {
		setProperty(PluginUtils.configureString(configElem, "@fieldName", true));
	}
	
}
