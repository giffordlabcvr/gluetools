package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"remove", "member"}, 
	description="Remove member sequences",
	docoptUsages={"<sourceName> <sequenceID>", "(-w <whereClause> | -a)"},
	metaTags={CmdMeta.updatesDatabase},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>  Qualify removed members",
	    "-a, --allMembers                               Remove all members"},
	furtherHelp=
	"The whereClause, if specified, qualifies which members are removed.\n"+
	"If allMembers is specified, all members will be removed from the alignment.\n"+
	"Examples:\n"+
	"  remove member localSource GH12325\n"+
	"  remove member -w \"sequence.source.name = 'local'\"\n"+
	"  remove member -w \"sequence.sequenceID like 'f%' and sequence.CUSTOM_FIELD = 'value1'\"\n"+
	"  remove member -w \"sequence.sequenceID = '3452467'\"\n"+
	"  remove member -a\n"+
	"Note: removing a sequence from the alignment does not delete it from the project.\n"+
	"Note: if a removed member is the reference of a child alignment, the child alignment's parent is unset."
) 
public class RemoveMemberCommand extends AlignmentModeCommand<DeleteResult> {

	public static final String SOURCE_NAME = "sourceName";
	public static final String SEQUENCE_ID = "sequenceID";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	
	private Optional<String> sourceName;
	private Optional<String> sequenceID;
	private Optional<Expression> whereClause;
	private Boolean allMembers;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, SOURCE_NAME, false));
		sequenceID = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, false));
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);
		if(!(
				(sourceName.isPresent() && sequenceID.isPresent() && !whereClause.isPresent() && !allMembers)||
				(!sourceName.isPresent() && !sequenceID.isPresent() && !whereClause.isPresent() && allMembers)||
				(!sourceName.isPresent() && !sequenceID.isPresent() && whereClause.isPresent() && !allMembers)
			)) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either both sourceName and sequenceID or whereClause or allMembers must be specified");
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
		} else if(allMembers) {
			List<AlignmentMember> members = alignment.getMembers();
			membersToDelete = new ArrayList<AlignmentMember>(members);
		} else {
			membersToDelete = Arrays.asList(GlueDataObject.lookup(objContext, AlignmentMember.class, 
					AlignmentMember.pkMap(getAlignmentName(), sourceName.get(), sequenceID.get())));
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
	
	@CompleterClass
	public static class Completer extends AlignmentModeCompleter {

		@SuppressWarnings("rawtypes")
		@Override
		public List<String> completionSuggestions(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty()) {
				return getMemberSources(cmdContext);
			} else {
				String arg0 = argStrings.get(0);
				if(argStrings.size() == 1 &&
						!Arrays.asList("-a", "--allMembers", "-w", "--whereClause").contains(arg0)) {
					Alignment almt = getAlignment(cmdContext);
					return getMemberSequenceIDs(arg0, almt);
				}
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}

	}

}
