package uk.ac.gla.cvr.gluetools.core.translationModification;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.translationModification.TranslationModifierException.Code;

@PluginClass(elemName="duplicateNucleotide")
public class DuplicateNucleotideRule extends ModifierRule {

	@Override
	protected void applyModifierRuleToNucleotides(List<Character> modifiedCharacters) {
		int segmentSize = modifiedCharacters.size();
		int segmentNtIndex = getSegmentNtIndex();
		if(segmentNtIndex > segmentSize) {
			throw new TranslationModifierException(Code.MODIFICATION_ERROR, "Cannot duplicate nucleotide at position "+segmentNtIndex+" as segment size is only "+segmentSize);
		}
		modifiedCharacters.add(segmentNtIndex-1, modifiedCharacters.get(segmentNtIndex-1));
	}

	@Override
	protected void applyModifierRuleToDependentPositions(List<DependentPosition> dependentPositions) {
		int segmentSize = dependentPositions.size();
		int segmentNtIndex = getSegmentNtIndex();
		if(segmentNtIndex > segmentSize) {
			throw new TranslationModifierException(Code.MODIFICATION_ERROR, "Cannot duplicate nucleotide at position "+segmentNtIndex+" as segment size is only "+segmentSize);
		}
		dependentPositions.add(segmentNtIndex-1, new DependentPosition(dependentPositions.get(segmentNtIndex-1).dependentRefNt));
	}
	
	
	
}
