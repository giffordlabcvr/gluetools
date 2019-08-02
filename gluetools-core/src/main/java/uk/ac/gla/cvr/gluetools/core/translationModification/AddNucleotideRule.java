package uk.ac.gla.cvr.gluetools.core.translationModification;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.translationModification.TranslationModifierException.Code;

public abstract class AddNucleotideRule extends ModifierRule {

	private char addedNt;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		String addedNtString = PluginUtils.configureStringProperty(configElem, "addedNt", true);
		if(addedNtString.length() != 1) {
			throw new TranslationModifierException(Code.CONFIG_ERROR, "The <addedNt> property must specify a single character");
		}
		this.addedNt = addedNtString.toUpperCase().charAt(0);
		if("ACGT".indexOf(addedNt) < 0) {
			throw new TranslationModifierException(Code.CONFIG_ERROR, "The <addedNt> property must specify A, C, G or T");
		}
	}
	
	protected char getAddedNt() {
		return addedNt;
	}

}
