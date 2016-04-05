package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.AlignmentException;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;

@CommandClass( 
		commandWords={"demote", "member"},
		docoptUsages={
				"<childAlmtName> (-m <sourceName> <sequenceID> | -w <whereClause> | -a)"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
			"-m, --member                                   Demote specific member",
			"-w <whereClause>, --whereClause <whereClause>  Qualify demoted members",
		    "-a, --allMembers                               Demote all members"},
		description="Move certain members to a child alignment",
		furtherHelp="The <childAlmtName> argument must specify a child of this alignment. "+
				"The member or members of this alignment specified are added to the child alignment. "+
				"The demoted members will also be removed from this alignment unless they are references of "+
				"some child of this alignment."
	) 
public class AlignmentDemoteMemberCommand extends AlignmentModeCommand<OkResult> {

	public static final String CHILD_ALMT_NAME = "childAlmtName";
	public static final String SOURCE_NAME = "sourceName";
	public static final String SEQUENCE_ID = "sequenceID";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	public static final String MEMBER = "member";
	
	private String childAlmtName;
	private Optional<String> sourceName;
	private Optional<String> sequenceID;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	private Boolean member;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		childAlmtName = PluginUtils.configureStringProperty(configElem, CHILD_ALMT_NAME, true);
		sourceName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, SOURCE_NAME, false));
		sequenceID = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, false));
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);
		member = PluginUtils.configureBooleanProperty(configElem, MEMBER, true);
		if(!(
				(sourceName.isPresent() && sequenceID.isPresent() && member && !whereClause.isPresent() && !allMembers)||
				(!sourceName.isPresent() && !sequenceID.isPresent() && !member && !whereClause.isPresent() && allMembers)||
				(!sourceName.isPresent() && !sequenceID.isPresent() && !member && whereClause.isPresent() && !allMembers)
			)) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either both sourceName and sequenceID or whereClause or allMembers must be specified");
	}

	
	@Override
	public OkResult execute(CommandContext cmdContext) {
		Alignment thisAlignment = lookupAlignment(cmdContext);
		Alignment childAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(childAlmtName));
		if(childAlignment.getParent() == null || !childAlignment.getParent().getName().equals(thisAlignment.getName())) {
			throw new AlignmentException(AlignmentException.Code.ALIGNMENT_NOT_CHILD_OF_PARENT, childAlmtName, thisAlignment.getName());
		}
		ReferenceSequence childRef = childAlignment.getRefSequence();
		Sequence childRefSeq = childRef.getSequence();
		AlignmentMember childRefMemberOfParent = 
				GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
						AlignmentMember.pkMap(childAlignment.getName(), childRefSeq.getSource().getName(), childRefSeq.getSequenceID()));

		List<QueryAlignedSegment> childRefToParentRefSegs = childRefMemberOfParent.segmentsAsQueryAlignedSegments();

		List<QueryAlignedSegment> parentRefToChildRefSegs = childRefToParentRefSegs
				.stream().map(qaseg -> qaseg.invert()).collect(Collectors.toList());

		
		List<AlignmentMember> membersToDemote = lookupMembers(cmdContext, whereClause, allMembers, sourceName, sequenceID);
		membersToDemote.forEach(memberToDemote -> {
			Sequence sequence = memberToDemote.getSequence();
			List<QueryAlignedSegment> memberToDemoteToParentRefSegs = memberToDemote.segmentsAsQueryAlignedSegments();
			List<QueryAlignedSegment> memberToDemoteToChildRefSegs = QueryAlignedSegment.translateSegments(memberToDemoteToParentRefSegs, parentRefToChildRefSegs);
			AlignmentMember memberOfChild = AlignmentAddMemberCommand.addMember(cmdContext, childAlignment, sequence);
			
			
			// if member is not the reference of a child of this alignment, delete it.
			if(!isReferenceOfSomeChild(thisAlignment, memberToDemote)) {
				GlueDataObject.delete(cmdContext, AlignmentMember.class, memberToDemote.pkMap(), true);
			}
		});
		cmdContext.commit();
		return new OkResult();
	}

	@CompleterClass
	public static final class Completer extends AlignmentMemberCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("childAlmtName", new VariableInstantiator() {
				
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					AlignmentMode almtMode = (AlignmentMode) cmdContext.peekCommandMode();
					SelectQuery selectQuery = new SelectQuery(Alignment.class, 
							ExpressionFactory.matchExp(Alignment.PARENT_NAME_PATH, almtMode.getAlignmentName()));
					List<Alignment> childAlmts = GlueDataObject.query(cmdContext, Alignment.class, selectQuery);
					return childAlmts.stream()
							.map(almt -> new CompletionSuggestion(almt.getName(), true))
							.collect(Collectors.toList());
				}
			});

		}
		
	}
	
}
