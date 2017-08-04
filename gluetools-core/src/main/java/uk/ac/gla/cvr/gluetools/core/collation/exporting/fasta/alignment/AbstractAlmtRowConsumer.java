package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;

public abstract class AbstractAlmtRowConsumer {

	public abstract void consumeAlmtRow(CommandContext cmdContext, Map<String, String> memberPkMap, 
			AlignmentMember almtMember, String alignmentRowString);
	
}
