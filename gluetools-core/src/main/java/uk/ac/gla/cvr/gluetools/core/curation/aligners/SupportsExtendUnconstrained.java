package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public interface SupportsExtendUnconstrained<R extends AlignerResult> {

	public default boolean supportsExtendUnconstrained() {
		return true;
	}

	public Map<Map<String, String>, List<QueryAlignedSegment>> extendUnconstrained(CommandContext cmdContext,Boolean preserveExistingRows,List<Map<String, String>> existingMembersPkMaps,List<Map<String, String>> recomputedMembersPkMaps);;

}
