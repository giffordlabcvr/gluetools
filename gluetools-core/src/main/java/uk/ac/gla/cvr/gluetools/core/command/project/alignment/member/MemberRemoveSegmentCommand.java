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

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
	commandWords={"remove", "segment"}, 
	docoptUsages={"<refStart> <refEnd> <memberStart> <memberEnd>", 
			"-a"},
	docoptOptions={"-a, --allSegments  Remove all segments"},
	metaTags = {},
	description="Remove a specific aligned segment or all of them", 
	furtherHelp="") 
public class MemberRemoveSegmentCommand extends MemberModeCommand<DeleteResult> {

	public static final String ALL_SEGMENTS = "allSegments";
	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";
	public static final String MEMBER_START = "memberStart";
	public static final String MEMBER_END = "memberEnd";
	
	private Optional<Integer> refStart;
	private Optional<Integer> refEnd;
	private Optional<Integer> memberStart;
	private Optional<Integer> memberEnd;
	private Boolean allSegments;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refStart = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, REF_START, false));
		refEnd = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, REF_END, false));
		memberStart = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MEMBER_START, false));
		memberEnd = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MEMBER_END, false));
		allSegments = PluginUtils.configureBooleanProperty(configElem, ALL_SEGMENTS, true);
		if( !(
				(refStart.isPresent() && refEnd.isPresent() && memberStart.isPresent() && memberEnd.isPresent() && !allSegments) || 
				(!refStart.isPresent() && !refEnd.isPresent() && !memberStart.isPresent() && !memberEnd.isPresent() && allSegments)
				)) {
			usageError();
		}
		
	}
	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either allSegments or both reference start/end and member start/end must be specified");
	}
	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		
		if(allSegments) {
			Expression allMemberSegments = 
					ExpressionFactory.matchExp(AlignedSegment.ALIGNMENT_NAME_PATH, getAlignmentName())
					.andExp(ExpressionFactory.matchExp(AlignedSegment.MEMBER_SOURCE_NAME_PATH, getSourceName()))
					.andExp(ExpressionFactory.matchExp(AlignedSegment.MEMBER_SEQUENCE_ID_PATH, getSequenceID()));
			List<AlignedSegment> segmentsToDelete = GlueDataObject.query(cmdContext, AlignedSegment.class, 
					new SelectQuery(AlignedSegment.class, allMemberSegments));
			int numDeleted = 0;
			for(AlignedSegment segment: segmentsToDelete) {
				DeleteResult result = GlueDataObject.delete(cmdContext, AlignedSegment.class, segment.pkMap(), true);
				numDeleted = numDeleted+result.getNumber();
			}
			cmdContext.commit();
			return new DeleteResult(AlignedSegment.class, numDeleted);
		} else {		
			DeleteResult result = GlueDataObject.delete(cmdContext, AlignedSegment.class, 
					AlignedSegment.pkMap(getAlignmentName(), getSourceName(), getSequenceID(), 
							refStart.get(), 
							refEnd.get(), 
							memberStart.get(), 
							memberEnd.get()), true);
			cmdContext.commit();
			return result;
		}
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
	}
}
