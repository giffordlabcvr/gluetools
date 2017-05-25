package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.IsoCountryUtils;
import uk.ac.gla.cvr.gluetools.utils.IsoCountryUtils.CodeStyle;

@PluginClass(elemName="isoCountryPropertyPopulator")
public class IsoCountryPropertyPopulatorRule extends BaseXmlPropertyPopulatorRule {

	private CodeStyle codeStyle;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
		super.configure(pluginConfigContext, configElem);
		this.codeStyle = PluginUtils.configureEnum(CodeStyle.class, configElem, "@codeStyle", true);
		populateProperty(pluginConfigContext, configElem);
		setValueConverters(IsoCountryUtils.isoCountryValueConverters(codeStyle));
		setMainExtractor(null);
	}

	protected void populateProperty(PluginConfigContext pluginConfigContext, Element configElem) {
		setProperty(PluginUtils.configureString(configElem, "@property", true));
	}
}
