package uk.ac.gla.cvr.gluetools.core.curation.aligners.querysequence;

import java.util.Map;

public class PersistentQuerySeqSpecifier extends QuerySeqSpecifier {

	private Map<String, String> seqIdToNts;

	public Map<String, String> getSequenceIdToNucleotides() {
		return seqIdToNts;
	}


}
