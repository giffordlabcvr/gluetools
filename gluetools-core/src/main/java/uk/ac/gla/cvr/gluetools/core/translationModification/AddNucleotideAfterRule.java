package uk.ac.gla.cvr.gluetools.core.translationModification;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.translationModification.TranslationModifierException.Code;

@PluginClass(elemName="addNucleotideAfter")
public class AddNucleotideAfterRule extends AddNucleotideRule {
	
	@Override
	protected void applyModifierRuleToNucleotides(List<Character> modifiedCharacters) {
		int segmentSize = modifiedCharacters.size();
		int segmentNtIndex = getSegmentNtIndex();
		if(segmentNtIndex > segmentSize) {
			throw new TranslationModifierException(Code.MODIFICATION_ERROR, "Cannot add nucleotide after position "+segmentNtIndex+" as segment size is only "+segmentSize);
		}
		modifiedCharacters.add(segmentNtIndex, getAddedNt());
	}

	@Override
	protected void applyModifierRuleToDependentPositions(List<DependentPosition> dependentPositions) {
		int segmentSize = dependentPositions.size();
		int segmentNtIndex = getSegmentNtIndex();
		if(segmentNtIndex > segmentSize) {
			throw new TranslationModifierException(Code.MODIFICATION_ERROR, "Cannot add dependent position after position "+segmentNtIndex+" as segment size is only "+segmentSize);
		}
		dependentPositions.add(segmentNtIndex, new DependentPosition(null));
	}

	
}
