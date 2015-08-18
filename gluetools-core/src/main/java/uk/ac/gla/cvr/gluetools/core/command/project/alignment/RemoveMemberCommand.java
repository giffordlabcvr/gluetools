package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"remove", "member"}, 
	description="Remove member sequences",
	docoptUsages={"(-w <whereClause> | -a)"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>  Qualify removed members",
	    "-a, --allMembers                               Remove all members"},
	furtherHelp=
	"The whereClause, if specified, qualifies which members are removed.\n"+
	"If allMembers is specified, all members will be removed from the alignment.\n"+
	"Examples:\n"+
	"  remove member -a\n"+
	"  remove member -w \"sequence.source.name = 'local'\"\n"+
	"  remove member -w \"sequence.sequenceID like 'f%' and sequence.CUSTOM_FIELD = 'value1'\"\n"+
	"  remove member -w \"sequence.sequenceID = '3452467'\"\n"+
	"Note: removing a sequence from the alignment does not delete it from the project.\n"+
	"Note: if a removed member is the reference of a child alignment, the child alignment's parent is unset."
) 
public class RemoveMemberCommand extends AlignmentModeCommand<DeleteResult> {

	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	
	private Optional<Expression> whereClause;
	private Optional<Boolean> allMembers;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, false));
		if(!whereClause.isPresent() && !allMembers.isPresent()) {
			throw new PluginConfigException(Code.CONFIG_CONSTRAINT_VIOLATION, "either whereClause or allMembers must be specified");
		}
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Alignment alignment = lookupAlignment(cmdContext);
		List<AlignmentMember> membersToDelete;
		if(whereClause.isPresent()) {
			Expression whereClauseExp = whereClause.get();
			whereClauseExp = whereClauseExp.andExp(ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignment.getName()));
			membersToDelete = GlueDataObject.query(objContext, AlignmentMember.class, new SelectQuery(AlignmentMember.class, whereClauseExp));
		} else {
			List<AlignmentMember> members = alignment.getMembers();
			membersToDelete = new ArrayList<AlignmentMember>(members);
		}
		membersToDelete.forEach(member -> {
			List<ReferenceSequence> referenceSequences = member.getSequence().getReferenceSequences();
			for(ReferenceSequence referenceSequence: referenceSequences) {
				List<Alignment> refSeqAlmts = referenceSequence.getAlignments();
				for(Alignment refSeqAlmt: refSeqAlmts) {
					Alignment parent = refSeqAlmt.getParent();
					if(parent != null && parent.getName().equals(alignment.getName())) {
						refSeqAlmt.setParent(null);
					}
				}
			}
			GlueDataObject.delete(objContext, AlignmentMember.class, member.pkMap());
			
		});
		cmdContext.commit();
		return new DeleteResult(AlignmentMember.class, membersToDelete.size());
	}
	

}
