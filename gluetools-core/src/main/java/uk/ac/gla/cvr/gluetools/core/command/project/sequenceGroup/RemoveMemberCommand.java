package uk.ac.gla.cvr.gluetools.core.command.project.sequenceGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.groupMember.GroupMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.sequenceGroup.SequenceGroup;


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
	"If allMembers is specified, all members will be removed from the group.\n"+
	"Examples:\n"+
	"  remove member localSource GH12325\n"+
	"  remove member -w \"sequence.source.name = 'local'\"\n"+
	"  remove member -w \"sequence.sequenceID like 'f%' and sequence.custom_field = 'value1'\"\n"+
	"  remove member -w \"sequence.sequenceID = '3452467'\"\n"+
	"  remove member -a\n"+
	"Note: removing a sequence from the group does not delete it from the project."
) 
public class RemoveMemberCommand extends GroupModeCommand<DeleteResult> {

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
		
		SequenceGroup group = lookupGroup(cmdContext);
		List<GroupMember> membersToDelete;
		if(whereClause.isPresent()) {
			Expression whereClauseExp = whereClause.get();
			whereClauseExp = whereClauseExp.andExp(ExpressionFactory.matchExp(GroupMember.GROUP_NAME_PATH, group.getName()));
			membersToDelete = GlueDataObject.query(cmdContext, GroupMember.class, new SelectQuery(GroupMember.class, whereClauseExp));
		} else if(allMembers) {
			List<GroupMember> members = group.getMembers();
			membersToDelete = new ArrayList<GroupMember>(members);
		} else {
			GroupMember almtMember = GlueDataObject.lookup(cmdContext, GroupMember.class, 
					GroupMember.pkMap(getGroupName(), sourceName.get(), sequenceID.get()), true);
			if(almtMember != null) {
				membersToDelete = Arrays.asList(almtMember);
			} else {
				membersToDelete = Collections.emptyList();
			}
		}
		int deleted = 0;
		for(GroupMember member : membersToDelete) {
			GlueDataObject.delete(cmdContext, GroupMember.class, member.pkMap(), true);
			deleted++;
		};
		if(deleted > 0) {
			group.setLastUpdateTime(System.currentTimeMillis());
		}
		cmdContext.commit();
		return new DeleteResult(GroupMember.class, membersToDelete.size());
	}
	
	@CompleterClass
	public static class Completer extends GroupMemberCompleter {}

}
