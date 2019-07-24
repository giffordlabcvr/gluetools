package uk.ac.gla.cvr.gluetools.core.translationModification;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class OutputAminoAcid implements Plugin {

	private List<Integer> dependentNtPositions = new ArrayList<Integer>();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		List<Element> dependentNtElems = GlueXmlUtils.findChildElements(configElem, "dependentNtPosition");
		if(dependentNtElems.size() == 0) {
			throw new TranslationModifierException(TranslationModifierException.Code.CONFIG_ERROR, "No dependentNtPositions defined");
		}
		for(Element dependentNtElem: dependentNtElems) {
			String textContent = dependentNtElem.getTextContent();
			int dependentNtPosition;
			try {
				dependentNtPosition = Integer.parseInt(textContent);
			} catch(NumberFormatException nfe) {
				throw new PluginConfigException(Code.PROPERTY_FORMAT_ERROR, "dependentNtPosition", "Not an integer", textContent);
			}
			if(dependentNtPositions.contains(dependentNtPosition)) {
				throw new TranslationModifierException(TranslationModifierException.Code.CONFIG_ERROR, "Duplicate dependentNtPositions");
			}
			if(dependentNtPosition < 0) {
				throw new TranslationModifierException(TranslationModifierException.Code.CONFIG_ERROR, "Invalid dependentNtPosition");
			}
			PluginUtils.setValidConfig(configElem, dependentNtElem);
			dependentNtPositions.add(dependentNtPosition);
		}
		dependentNtPositions.sort(null);
	}

	public List<Integer> getDependentNtPositions() {
		return dependentNtPositions;
	}

	
}
