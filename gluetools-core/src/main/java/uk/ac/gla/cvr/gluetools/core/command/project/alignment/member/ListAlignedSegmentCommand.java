package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;


@CommandClass(
		commandWords={"list", "segment"}, 
		docoptUsages={""},
		description="List the aligned segments") 
public class ListAlignedSegmentCommand extends MemberModeCommand<ListAlignedSegmentCommand.ListAlignedSegmentResult> {

	@Override
	public ListAlignedSegmentResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(AlignedSegment.ALIGNMENT_NAME_PATH, getAlignmentName());
		exp = exp.andExp(ExpressionFactory.matchExp(AlignedSegment.MEMBER_SOURCE_NAME_PATH, getSourceName()));
		exp = exp.andExp(ExpressionFactory.matchExp(AlignedSegment.MEMBER_SEQUENCE_ID_PATH, getSequenceID()));
		
		List<AlignedSegment> segments = 
				GlueDataObject.query(cmdContext.getObjectContext(), AlignedSegment.class, 
						new SelectQuery(AlignedSegment.class, exp));
		return new ListAlignedSegmentResult(segments);
	}

	public static class ListAlignedSegmentResult extends ListResult {

		public ListAlignedSegmentResult(List<AlignedSegment> segments) {
			super(AlignedSegment.class, segments, 
					Arrays.asList(
							AlignedSegment.REF_START_PROPERTY, 
							AlignedSegment.REF_END_PROPERTY, 
							AlignedSegment.MEMBER_START_PROPERTY, 
							AlignedSegment.MEMBER_END_PROPERTY));
		}
		
		public List<QueryAlignedSegment> asQueryAlignedSegments() {
			List<Map<String, Object>> listOfMaps = super.asListOfMaps();
			List<QueryAlignedSegment> queryAlignedSegments = listOfMaps.stream()
					.map(m -> new QueryAlignedSegment(
							(Integer) m.get(AlignedSegment.REF_START_PROPERTY),
							(Integer) m.get(AlignedSegment.REF_END_PROPERTY),
							(Integer) m.get(AlignedSegment.MEMBER_START_PROPERTY),
							(Integer) m.get(AlignedSegment.MEMBER_END_PROPERTY)))
					.collect(Collectors.toList());
			return queryAlignedSegments;
		}
	}
	
}
