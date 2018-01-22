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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberMode;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"member"},
	docoptUsages={"<sourceName> <sequenceID>", 
				"-w <whereClause>"},
	docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Specify based on field values"},
	description="Enter command mode for an alignment member", 
	furtherHelp=
	"The optional whereClause allows a member to be specified via its field values.\n"+
	"If this query returns multiple or zero members, the command fails.\n"+
	"Examples:\n"+
	"  member -w \"sequence.gb_primary_accession = 'GR195721'\"\n"+
	"  member mySource 12823121") 
@EnterModeCommandClass(
		commandFactoryClass = MemberModeCommandFactory.class, 
		modeDescription = "AlignmentMember mode")
public class MemberCommand extends AlignmentModeCommand<OkResult>  {

	public static final String SEQUENCE_ID = "sequenceID";
	public static final String SOURCE_NAME = "sourceName";
	private String sourceName;
	private String sequenceID;
	private Expression whereClause;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, false);
		sequenceID = PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, false);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false);
		
		if(whereClause == null) {
			if(sourceName == null || sequenceID == null) {
				usageError();
			}
		} else {
			if(sourceName != null || sequenceID != null) {
				usageError();
			}
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, 
				"Either whereClause or both sourceName and sequenceID must be specified");
	}
	
	@Override
	public OkResult execute(CommandContext cmdContext) {
		Sequence sequence;
		if(whereClause == null) {
			sequence = GlueDataObject.lookup(cmdContext, Sequence.class, 
					Sequence.pkMap(sourceName, sequenceID));
		} else {
			Expression exp = whereClause.andExp(ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, getAlignmentName()));
			SelectQuery selectQuery = new SelectQuery(AlignmentMember.class, exp);
			List<AlignmentMember> members = GlueDataObject.query(cmdContext, AlignmentMember.class, selectQuery);
			int numMembers = members.size();
			if(numMembers == 1) {
				sequence = members.get(0).getSequence();
			} else if(numMembers == 0) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "Query returned no members.");
			} else {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "Query returned multiple members.");
			} 
		}
		Project project = getAlignmentMode(cmdContext).getProject();
		cmdContext.pushCommandMode(new MemberMode(project, this, getAlignmentName(), sequence.getSource().getName(), sequence.getSequenceID()));
		return CommandResult.OK;
	}
	
	
	
	@CompleterClass
	public static class Completer extends AlignmentMemberCompleter {}

	

}
