package uk.ac.gla.cvr.gluetools.core.translationModification;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.translationModification.TranslationModifierException.Code;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="translationModifier",
description="Specifies how certain nucleotide locations are modified before protein translation, e.g. ribosomal slippage or RNA editing")
public class TranslationModifier extends ModulePlugin<TranslationModifier> {

	// we expect the segment to be this long.
	private int segmentNtLength;
	
	private List<ModifierRule> modifierRules = new ArrayList<ModifierRule>();
	private List<OutputAminoAcid> outputAminoAcids = new ArrayList<OutputAminoAcid>();
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.segmentNtLength = PluginUtils.configureIntProperty(configElem, "segmentNtLength", 1, true, null, false, true);
		Element preTranslationNtModificationsElem = PluginUtils.findConfigElement(configElem, "preTranslationNtModifications");
		if(preTranslationNtModificationsElem != null) {
			ModifierRuleFactory modifierRuleFactory = PluginFactory.get(ModifierRuleFactory.creator);
			String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(modifierRuleFactory.getElementNames());
			List<Element> modifierRuleElems = PluginUtils.findConfigElements(preTranslationNtModificationsElem, alternateElemsXPath);
			modifierRules = modifierRuleFactory.createFromElements(pluginConfigContext, modifierRuleElems);
		}
		List<Element> outputAaElems = PluginUtils.findConfigElements(configElem, "outputAminoAcid", 1, null);
		for(Element outputAaElem: outputAaElems) {
			OutputAminoAcid outputAminoAcid = new OutputAminoAcid();
			PluginFactory.configurePlugin(pluginConfigContext, outputAaElem, outputAminoAcid);
			outputAminoAcids.add(outputAminoAcid);
		}
		Set<Integer> outputAaStartPoints = new LinkedHashSet<Integer>();
		for(OutputAminoAcid outputAminoAcid: outputAminoAcids) {
			int startRefNt = outputAminoAcid.getDependentNtPositions().get(0);
			if(outputAaStartPoints.contains(startRefNt)) {
				throw new TranslationModifierException(Code.CONFIG_ERROR, "Multiple output amino acids have same first dependent position "+startRefNt);
			}
			outputAaStartPoints.add(startRefNt);
		}
	}

	public int getSegmentNtLength() {
		return segmentNtLength;
	}
	
	public void applyModifier(List<Character> nts) {
		int segmentSize = nts.size();
		if(segmentSize != segmentNtLength) {
			throw new TranslationModifierException(Code.MODIFICATION_ERROR, 
					"Cannot apply pre-translation nucleotide modifier as segment size "+segmentSize+" does not match expected size "+segmentNtLength);
		}
		for(ModifierRule modifierRule: modifierRules) {
			modifierRule.applyModifierRule(nts);
		}
		int expectedLength = outputAminoAcids.size() * 3;
		if(nts.size() != expectedLength) {
			throw new TranslationModifierException(Code.MODIFICATION_ERROR, 
					"After pre-translation nucleotide translation modification the segment length was "+nts.size()+" but the expected length was "+expectedLength);
		}
	}

	public List<OutputAminoAcid> getOutputAminoAcids() {
		return outputAminoAcids;
	}
	
}
