package uk.ac.gla.cvr.gluetools.core.translationModification;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.translationModification.ModifierRule.DependentPosition;
import uk.ac.gla.cvr.gluetools.core.translationModification.TranslationModifierException.Code;

@PluginClass(elemName="addNucleotideBefore")
public class AddNucleotideBeforeRule extends AddNucleotideRule {

	@Override
	protected void applyModifierRuleToNucleotides(List<Character> modifiedCharacters) {
		int segmentSize = modifiedCharacters.size();
		int segmentNtIndex = getSegmentNtIndex();
		if(segmentNtIndex > segmentSize) {
			throw new TranslationModifierException(Code.MODIFICATION_ERROR, "Cannot add nucleotide before position "+segmentNtIndex+" as segment size is only "+segmentSize);
		}
		modifiedCharacters.add(segmentNtIndex-1, getAddedNt());
	}

	@Override
	protected void applyModifierRuleToDependentPositions(List<DependentPosition> dependentPositions) {
		int segmentSize = dependentPositions.size();
		int segmentNtIndex = getSegmentNtIndex();
		if(segmentNtIndex > segmentSize) {
			throw new TranslationModifierException(Code.MODIFICATION_ERROR, "Cannot add dependent position before position "+segmentNtIndex+" as segment size is only "+segmentSize);
		}
		dependentPositions.add(segmentNtIndex-1, new DependentPosition(null));
	}

	

	
}
