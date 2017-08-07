package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;

public abstract class AbstractSequenceConsumer {

	public abstract void consumeSequence(CommandContext cmdContext, Sequence sequence);
	
}
