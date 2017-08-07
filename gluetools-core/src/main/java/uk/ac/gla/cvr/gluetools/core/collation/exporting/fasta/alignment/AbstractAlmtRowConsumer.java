package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;

public abstract class AbstractAlmtRowConsumer {

	public abstract void consumeAlmtRow(CommandContext cmdContext, AlignmentMember almtMember, String alignmentRowString);
	
}
