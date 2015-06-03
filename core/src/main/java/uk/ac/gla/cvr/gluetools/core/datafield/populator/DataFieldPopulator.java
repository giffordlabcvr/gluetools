package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

/**
 * A plugin which can populate the data fields of a collated sequence.
 */
public interface DataFieldPopulator extends Plugin {

	/**
	 * Set values for zero or more data fields of the given sequence.
	 */
	public void populate(CollatedSequence sequence) throws DataFieldPopulatorException;
}
