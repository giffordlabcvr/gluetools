package uk.ac.gla.cvr.gluetools.core.translationModification;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class ModifierRule implements Plugin {

	private int segmentNtIndex;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.segmentNtIndex = PluginUtils.configureIntProperty(configElem, "segmentNtIndex", 1, true, null, false, true);
	}
	
	protected int getSegmentNtIndex() {
		return this.segmentNtIndex;
	}

	protected static class DependentPosition {
		Integer dependentRefNt;

		public DependentPosition(Integer dependentRefNt) {
			super();
			this.dependentRefNt = dependentRefNt;
		}
	}
		
	protected abstract void applyModifierRuleToNucleotides(List<Character> modifiedCharacters);

	protected abstract void applyModifierRuleToDependentPositions(List<DependentPosition> dependentPositions);
}
