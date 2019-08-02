package uk.ac.gla.cvr.gluetools.core.translationModification;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.translationModification.TranslationModifierException.Code;

@PluginClass(elemName="deleteNucleotide")
public class DeleteNucleotideRule extends ModifierRule {

	@Override
	protected void applyModifierRuleToNucleotides(List<Character> modifiedCharacters) {
		int segmentSize = modifiedCharacters.size();
		int segmentNtIndex = getSegmentNtIndex();
		if(segmentNtIndex > segmentSize) {
			throw new TranslationModifierException(Code.MODIFICATION_ERROR, "Cannot delete nucleotide "+segmentNtIndex+" as segment size is only "+segmentSize);
		}
		modifiedCharacters.remove(segmentNtIndex-1);
	}

	@Override
	protected void applyModifierRuleToDependentPositions(List<DependentPosition> dependentPositions) {
		int segmentSize = dependentPositions.size();
		int segmentNtIndex = getSegmentNtIndex();
		if(segmentNtIndex > segmentSize) {
			throw new TranslationModifierException(Code.MODIFICATION_ERROR, "Cannot delete dependent position "+segmentNtIndex+" as segment size is only "+segmentSize);
		}
		dependentPositions.remove(segmentNtIndex-1);
	}

}
