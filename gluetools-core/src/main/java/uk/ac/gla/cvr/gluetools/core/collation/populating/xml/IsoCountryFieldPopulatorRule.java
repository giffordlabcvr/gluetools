package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="isoCountryFieldPopulator", deprecated=true,
deprecationWarning="Plugin element \"isoCountryFieldPopulator\" with attribute \"fieldName\" "
+"is deprecated, please use element \"isoCountryPropertyPopulator\" with attribute \"property\"")

public class IsoCountryFieldPopulatorRule extends IsoCountryPropertyPopulatorRule {

	protected void populateProperty(PluginConfigContext pluginConfigContext, Element configElem) {
		setProperty(PluginUtils.configureString(configElem, "@fieldName", true));
	}

}
