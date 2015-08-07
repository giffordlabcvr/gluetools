package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;

@CommandClass( 
	commandWords={"clear", "segment"}, 
	docoptUsages={""},
	description="Remove all aligned segments", 
	furtherHelp="") 
public class ClearAlignedSegmentCommand extends MemberModeCommand<DeleteResult> {

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Expression allMemberSegments = 
				ExpressionFactory.matchExp(AlignedSegment.ALIGNMENT_NAME_PATH, getAlignmentName())
				.andExp(ExpressionFactory.matchExp(AlignedSegment.MEMBER_SOURCE_NAME_PATH, getSourceName()))
				.andExp(ExpressionFactory.matchExp(AlignedSegment.MEMBER_SEQUENCE_ID_PATH, getSequenceID()));
		List<AlignedSegment> segmentsToDelete = GlueDataObject.query(objContext, AlignedSegment.class, 
				new SelectQuery(AlignedSegment.class, allMemberSegments));
		int numDeleted = 0;
		for(AlignedSegment segment: segmentsToDelete) {
			DeleteResult result = GlueDataObject.delete(objContext, AlignedSegment.class, segment.pkMap());
			numDeleted = numDeleted+result.getNumber();
		}
		cmdContext.commit();
		return new DeleteResult(AlignedSegment.class, numDeleted);
	}

}
