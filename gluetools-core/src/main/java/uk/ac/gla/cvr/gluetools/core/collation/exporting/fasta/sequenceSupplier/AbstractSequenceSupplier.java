package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;

public abstract class AbstractSequenceSupplier {

	public AbstractSequenceSupplier() {
	}

	public abstract int countSequences(CommandContext cmdContext);
	
	public abstract List<Sequence> supplySequences(CommandContext cmdContext, int offset, int number);
	
}
