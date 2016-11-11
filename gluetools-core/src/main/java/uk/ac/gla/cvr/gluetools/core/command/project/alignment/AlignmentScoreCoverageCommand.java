package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@CommandClass( 
		commandWords={"score", "coverage"}, 
		description="Score member sequences for coverage of the genome",
		docoptUsages={"(-w <whereClause> | -a)"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
			"-w <whereClause>, --whereClause <whereClause>  Qualify selected members",
		    "-a, --allMembers                               Select all members"},
		furtherHelp=
				"This command may only be used on unconstrained alignments.\n"+
				"Each column in the alignment is considered.\n"+
				"The weight of each column equals the number of selected alignment members which cover it.\n"+
				"Each selected member's score is the sum of weights of all columns covered by that member.\n"+
				"The whereClause, if specified, qualifies which members are scored.\n"+
				"If allMembers is specified, all members will be scored.\n"+
				"Examples:\n"+
				"  score coverage -w \"sequence.source.name = 'local'\"\n"+
				"  score coverage -a"
	) 
public class AlignmentScoreCoverageCommand extends AlignmentModeCommand<AlignmentScoreCoverageResult> {

	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);
		if(!whereClause.isPresent() && !allMembers) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or allMembers must be specified");
	}

	@Override
	public AlignmentScoreCoverageResult execute(CommandContext cmdContext) {
 		Alignment alignment = lookupAlignment(cmdContext);
		if(alignment.isConstrained()) {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Command may only be used on unconstrained alignments.");
		}
		List<AlignmentMember> selectedMembers = null;
		if(whereClause.isPresent()) {
			Expression whereClauseExp = whereClause.get();
			whereClauseExp = whereClauseExp.andExp(ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignment.getName()));
			selectedMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, new SelectQuery(AlignmentMember.class, whereClauseExp));
		} else {
			if(allMembers) {
				List<AlignmentMember> members = alignment.getMembers();
				selectedMembers = new ArrayList<AlignmentMember>(members);
			}
		}
		int minColIndex = Integer.MAX_VALUE;
		int maxColIndex = Integer.MIN_VALUE;
		for(AlignmentMember almtMember : selectedMembers) {
			List<AlignedSegment> alignedSegments = almtMember.getAlignedSegments();
			minColIndex = Math.min(minColIndex, ReferenceSegment.minRefStart(alignedSegments));
			maxColIndex = Math.max(maxColIndex, ReferenceSegment.maxRefEnd(alignedSegments));
		}
		TIntIntMap columnWeights = new TIntIntHashMap();
		for(int i = minColIndex; i <= maxColIndex; i++) {
			columnWeights.put(i, 0);
		}
		for(AlignmentMember almtMember : selectedMembers) {
			List<AlignedSegment> alignedSegments = almtMember.getAlignedSegments();
			for(AlignedSegment seg: alignedSegments) {
				for(int j = seg.getRefStart(); j <= seg.getRefEnd(); j++) {
					columnWeights.adjustValue(j, 1);
				}
			}
		}
		ArrayList<AlignmentCoverageScore> scores = new ArrayList<AlignmentCoverageScore>();
		for(AlignmentMember almtMember : selectedMembers) {
			double memberScore = 0.0;
			List<AlignedSegment> alignedSegments = almtMember.getAlignedSegments();
			for(AlignedSegment seg: alignedSegments) {
				for(int j = seg.getRefStart(); j <= seg.getRefEnd(); j++) {
					int columnWeight = columnWeights.get(j);
					if(columnWeight > 1) {
						memberScore += columnWeight;
					}
				}
			}
			scores.add(new AlignmentCoverageScore(almtMember, memberScore));
		}
		// report by descending score
		scores.sort(new Comparator<AlignmentCoverageScore>() {
			@Override
			public int compare(AlignmentCoverageScore o1, AlignmentCoverageScore o2) {
				return Double.compare(o2.getScore(), o1.getScore());
			}
		});
		return new AlignmentScoreCoverageResult(scores);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {}
	
}
