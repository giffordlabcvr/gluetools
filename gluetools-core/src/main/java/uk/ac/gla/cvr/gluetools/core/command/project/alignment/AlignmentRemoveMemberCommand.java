/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"remove", "member"}, 
	description="Remove member sequences",
	docoptUsages={"(-m <sourceName> <sequenceID> | -w <whereClause> | -a)"},
	metaTags={CmdMeta.updatesDatabase},
	docoptOptions={
		"-m, --member                                   Remove specific member",
		"-w <whereClause>, --whereClause <whereClause>  Qualify removed members",
	    "-a, --allMembers                               Remove all members"},
	furtherHelp=
	"The whereClause, if specified, qualifies which members are removed.\n"+
	"If allMembers is specified, all members will be removed from the alignment.\n"+
	"Examples:\n"+
	"  remove member -m localSource GH12325\n"+
	"  remove member -w \"sequence.source.name = 'local'\"\n"+
	"  remove member -w \"sequence.sequenceID like 'f%' and sequence.custom_field = 'value1'\"\n"+
	"  remove member -w \"sequence.sequenceID = '3452467'\"\n"+
	"  remove member -a\n"+
	"Note: removing a sequence from the alignment does not delete it from the project.\n"+
	"Note: if a member is the reference of a child alignment, it is not removed."
) 
public class AlignmentRemoveMemberCommand extends AlignmentModeCommand<DeleteResult> {

	public static final String SOURCE_NAME = "sourceName";
	public static final String SEQUENCE_ID = "sequenceID";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	public static final String MEMBER = "member";

	
	private Optional<String> sourceName;
	private Optional<String> sequenceID;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	private Boolean member;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
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
	public DeleteResult execute(CommandContext cmdContext) {
		
		Alignment alignment = lookupAlignment(cmdContext);
		List<AlignmentMember> membersToRemove = lookupMembers(cmdContext, whereClause, allMembers, sourceName, sequenceID);
		removeMembers(cmdContext, alignment, membersToRemove);
		cmdContext.commit();
		return new DeleteResult(AlignmentMember.class, membersToRemove.size());
	}

	
	public static void removeMembers(CommandContext cmdContext, Alignment alignment,
			List<AlignmentMember> membersToRemove) {
		membersToRemove.forEach(member -> {
			// if member is the reference of a child of this alignment, do not delete it.
			if(!isReferenceOfSomeChild(alignment, member)) {
				GlueDataObject.delete(cmdContext, AlignmentMember.class, member.pkMap(), true);
			}
		});
	}
	
	@CompleterClass
	public static class Completer extends AlignmentMemberCompleter {}

}
