package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public interface SupportsComputeUnconstrained {

	public Map<Map<String, String>, List<QueryAlignedSegment>> computeUnconstrained(
			CommandContext cmdContext, String alignmentName);

}
