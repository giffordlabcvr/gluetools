package uk.ac.gla.cvr.gluetools.core.preTranslationModification;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.blastRecogniser.CategoryResultResolverFactory;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.preTranslationModification.PreTranslationModifierException.Code;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="preTranslationModifier",
description="Specifies how certain nucleotide locations are modified before protein translation, e.g. ribosomal slippage or RNA editing")
public class PreTranslationModifier extends ModulePlugin<PreTranslationModifier> {

	// we expect the segment to be this long.
	private int segmentNtLength;
	
	private List<ModifierRule> modifierRules;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.segmentNtLength = PluginUtils.configureIntProperty(configElem, "segmentNtLength", 1, true, null, false, true);
		ModifierRuleFactory modifierRuleFactory = PluginFactory.get(ModifierRuleFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(modifierRuleFactory.getElementNames());
		List<Element> modifierRuleElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		modifierRules = modifierRuleFactory.createFromElements(pluginConfigContext, modifierRuleElems);
		if(modifierRules.isEmpty()) {
			throw new PreTranslationModifierException(Code.CONFIG_ERROR, "No modifier rules defined");
		}
	}

	public int getSegmentNtLength() {
		return segmentNtLength;
	}
	
	public void applyModifier(List<Character> nts) {
		int segmentSize = nts.size();
		if(segmentSize != segmentNtLength) {
			throw new PreTranslationModifierException(Code.MODIFICATION_ERROR, 
					"Cannot apply pre-translation modifier as segment size "+segmentSize+" does not match expected size "+segmentNtLength);
		}
		for(ModifierRule modifierRule: modifierRules) {
			modifierRule.applyModifierRule(nts);
		}
		if(nts.size() % 3 != 0) {
			throw new PreTranslationModifierException(Code.MODIFICATION_ERROR, 
					"After pre-translation modification the segment length was "+nts.size()+" which is not a multiple of 3");
		}
	}
	
}
