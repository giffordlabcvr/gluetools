package uk.ac.gla.cvr.gluetools.core.preTranslationModification;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.preTranslationModification.PreTranslationModifierException.Code;

@PluginClass(elemName="deleteNucleotide")
public class DeleteNucleotideRule extends ModifierRule {

	@Override
	protected void applyModifierRule(List<Character> modifiedCharacters) {
		int segmentSize = modifiedCharacters.size();
		int segmentNtIndex = getSegmentNtIndex();
		if(segmentNtIndex > segmentSize) {
			throw new PreTranslationModifierException(Code.MODIFICATION_ERROR, "Cannot delete nucleotide "+segmentNtIndex+" as segment size is only "+segmentSize);
		}
		modifiedCharacters.remove(segmentNtIndex-1);
	}

}
