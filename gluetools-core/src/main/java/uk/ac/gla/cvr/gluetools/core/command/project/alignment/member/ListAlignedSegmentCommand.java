package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;


@CommandClass(
	commandWords={"list", "segment"}, 
	docoptUsages={""},
	description="List the aligned segments") 
public class ListAlignedSegmentCommand extends MemberModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(AlignedSegment.ALIGNMENT_NAME_PATH, getAlignmentName());
		exp = exp.andExp(ExpressionFactory.matchExp(AlignedSegment.MEMBER_SOURCE_NAME_PATH, getSourceName()));
		exp = exp.andExp(ExpressionFactory.matchExp(AlignedSegment.MEMBER_SEQUENCE_ID_PATH, getSequenceID()));
		return CommandUtils.runListCommand(cmdContext, AlignedSegment.class, new SelectQuery(AlignedSegment.class, exp));
	}

}
