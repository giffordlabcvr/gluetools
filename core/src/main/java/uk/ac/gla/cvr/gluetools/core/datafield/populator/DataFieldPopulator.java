package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;

/**
 * A plugin which can populate the data fields of a project collated sequence.
 */
public abstract class DataFieldPopulator {

	/**
	 * Set values for zero or more data fields of the given sequence.
	 */
	public abstract void populate(CollatedSequence sequence) throws DataFieldPopulatorException;
}
