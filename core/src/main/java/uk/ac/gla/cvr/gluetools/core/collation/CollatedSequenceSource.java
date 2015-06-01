package uk.ac.gla.cvr.gluetools.core.collation;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;

/**
 * A module that can produce collated sequences.
 * 
 * @author joshsinger
 *
 */
public interface CollatedSequenceSource<S extends CollatedSequence> {

	
	public void updateSequences() throws SequenceCollationException;
	
	public List<S> getSequences();
}
