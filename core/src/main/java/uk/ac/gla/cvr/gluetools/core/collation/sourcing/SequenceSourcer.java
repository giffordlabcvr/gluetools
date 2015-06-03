package uk.ac.gla.cvr.gluetools.core.collation.sourcing;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

/**
 * A plugin that can obtain collated sequences from a source.
 * 
 */
public interface SequenceSourcer extends Plugin {

	/**
	 * Get a list of IDs of sequences which this source can retrieve.
	 * The IDs are valid within the source.
	 * @throws SequenceSourcerException
	 */
	public List<String> getSequenceIDs() throws SequenceSourcerException;
	
	/**
	 * Return a unique identifier for this source.
	 */
	public String getSourceUniqueID();
	
	/**
	 * Given a list of sequence IDs valid within the source, retrieve them.
	 * The retrieved sequences will not have a project or data fields.
	 * @param sequenceIDs
	 * @throws SequenceSourcerException
	 */
	public List<CollatedSequence> retrieveSequences(List<String> sequenceIDs) throws SequenceSourcerException;
	
}
