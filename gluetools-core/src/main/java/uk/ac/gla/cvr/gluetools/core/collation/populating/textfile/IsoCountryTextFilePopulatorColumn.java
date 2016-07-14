package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.IsoCountryUtils;
import uk.ac.gla.cvr.gluetools.utils.IsoCountryUtils.CodeStyle;

@PluginClass(elemName="isoCountryTextFileColumn")
public class IsoCountryTextFilePopulatorColumn extends BaseTextFilePopulatorColumn {

	private CodeStyle codeStyle;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
		super.configure(pluginConfigContext, configElem);
		this.codeStyle = PluginUtils.configureEnum(CodeStyle.class, configElem, "@codeStyle", true);
		setValueConverters(IsoCountryUtils.isoCountryValueConverters(codeStyle));
		setMainExtractor(null);
	}

}
