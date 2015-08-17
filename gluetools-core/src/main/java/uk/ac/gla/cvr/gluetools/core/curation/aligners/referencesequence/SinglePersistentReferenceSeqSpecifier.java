package uk.ac.gla.cvr.gluetools.core.curation.aligners.referencesequence;

import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;

public class SinglePersistentReferenceSeqSpecifier extends ReferenceSeqSpecifier {

	private Map<String, String> seqIdToNts;
	
	public SinglePersistentReferenceSeqSpecifier(CommandContext cmdContext, String refnName) {
	}

	@Override
	public Map<String, String> getSequenceIdToNucleotides() {
		return seqIdToNts;
	}
	

}
