package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public abstract class PopulatorRule implements Plugin {

	public abstract void execute(CollatedSequence collatedSequence, Node node);

	
}
