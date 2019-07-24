package uk.ac.gla.cvr.gluetools.core.translationModification;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.translationModification.TranslationModifierException.Code;

@PluginClass(elemName="addNucleotideBefore")
public class AddNucleotideBeforeRule extends AddNucleotideRule {

	@Override
	protected void applyModifierRule(List<Character> modifiedCharacters) {
		int segmentSize = modifiedCharacters.size();
		int segmentNtIndex = getSegmentNtIndex();
		if(segmentNtIndex > segmentSize) {
			throw new TranslationModifierException(Code.MODIFICATION_ERROR, "Cannot add nucleotide before position "+segmentNtIndex+" as segment size is only "+segmentSize);
		}
		modifiedCharacters.add(segmentNtIndex-1, getAddedNt());
	}

}
