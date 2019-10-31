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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@CommandClass(
		commandWords={"translate", "segment"}, 
		description = "Apply the member's homology to translate a specific segment", 
		docoptUsages = { "<memberNtStart> <memberNtEnd> [-q]"},
		docoptOptions={
				"-q, --queryAlignedSegments                     Return list of queryAlignedSegment objects",
		},
		furtherHelp = 
		"Supplied coordinate range on the member sequence will be translated to the alignment's coordinate space",
		metaTags = {}	
)
public class MemberTranslateSegmentCommand extends MemberModeCommand<CommandResult> {


	public static final String MEMBER_NT_START = "memberNtStart";
	public static final String MEMBER_NT_END = "memberNtEnd";
	private static final String QUERY_ALIGNED_SEGMENTS = "queryAlignedSegments";
	
	private int memberNtStart;
	private int memberNtEnd;
	private boolean queryAlignedSegments;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.memberNtStart = PluginUtils.configureIntProperty(configElem, MEMBER_NT_START, 1, true, null, false, true);
		this.memberNtEnd = PluginUtils.configureIntProperty(configElem, MEMBER_NT_END, 1, true, null, false, true);
		this.queryAlignedSegments = PluginUtils.configureBooleanProperty(configElem, QUERY_ALIGNED_SEGMENTS, true);
	}
	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		AlignmentMember almtMember = lookupMember(cmdContext);
		if(memberNtStart > memberNtEnd) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "<memberNtStart> may not be greater than <memberNtEnd>");
		}
		int seqLength = almtMember.getSequence().getSequenceObject().getNucleotides(cmdContext).length();
		if(memberNtEnd > seqLength) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "<memberNtEnd> may not be greater than sequence length of "+seqLength);
		}
		List<QueryAlignedSegment> memberToSelf = Arrays.asList(new QueryAlignedSegment(memberNtStart, memberNtEnd, memberNtStart, memberNtEnd));
		List<QueryAlignedSegment> memberToAlmtSegs = almtMember.segmentsAsQueryAlignedSegments();

		List<QueryAlignedSegment> translatedMemberSegs = QueryAlignedSegment.translateSegments(memberToSelf, memberToAlmtSegs);
		
		if(queryAlignedSegments) {
			return new QueryAlignedSegment.QueryAlignedSegmentsResult(translatedMemberSegs);
		} else {
			return new MemberQueryAlignedSegmentsTableResult(translatedMemberSegs);
		}
	}

	@CompleterClass
	public static final class Completer extends FeatureOfRelatedRefCompleter {}

	
}
