package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public interface IAlignmentColumnsSelector {

	public List<ReferenceSegment> selectAlignmentColumns(CommandContext cmdContext);
	
	public String getRelatedRefName();
	
}
