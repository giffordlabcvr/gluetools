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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;


@CommandClass(
		commandWords={"list", "segment"}, 
		docoptUsages={"[-q]"},
		docoptOptions={"-q, --queryAlignedSegments  Return list of queryAlignedSegment objects"},
		description="List the aligned segments") 
public class MemberListSegmentCommand extends MemberModeCommand<CommandResult> {

	private static final String QUERY_ALIGNED_SEGMENTS = "queryAlignedSegments";
	private boolean queryAlignedSegments;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.queryAlignedSegments = PluginUtils.configureBooleanProperty(configElem, QUERY_ALIGNED_SEGMENTS, true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(AlignedSegment.ALIGNMENT_NAME_PATH, getAlignmentName());
		exp = exp.andExp(ExpressionFactory.matchExp(AlignedSegment.MEMBER_SOURCE_NAME_PATH, getSourceName()));
		exp = exp.andExp(ExpressionFactory.matchExp(AlignedSegment.MEMBER_SEQUENCE_ID_PATH, getSequenceID()));
		
		List<AlignedSegment> segments = 
				GlueDataObject.query(cmdContext, AlignedSegment.class, 
						new SelectQuery(AlignedSegment.class, exp));
		segments = IReferenceSegment.sortByRefStart(segments, ArrayList::new);
		
		if(queryAlignedSegments) {
			return new QueryAlignedSegment.QueryAlignedSegmentsResult(segments.stream()
					.map(AlignedSegment::asQueryAlignedSegment).collect(Collectors.toList()));
		} else {
			return new ListAlignedSegmentResult(cmdContext, segments);
		}
	}

	public static class ListAlignedSegmentResult extends ListResult {

		public ListAlignedSegmentResult(CommandContext cmdContext, List<AlignedSegment> segments) {
			super(cmdContext, AlignedSegment.class, segments, 
					Arrays.asList(
							AlignedSegment.REF_START_PROPERTY, 
							AlignedSegment.REF_END_PROPERTY, 
							AlignedSegment.MEMBER_START_PROPERTY, 
							AlignedSegment.MEMBER_END_PROPERTY));
		}
		
		public List<QueryAlignedSegment> asQueryAlignedSegments() {
			List<Map<String, Object>> listOfMaps = super.asListOfMaps();
			List<QueryAlignedSegment> queryAlignedSegments = listOfMaps.stream()
					.map(m -> new QueryAlignedSegment(
							(Integer) m.get(AlignedSegment.REF_START_PROPERTY),
							(Integer) m.get(AlignedSegment.REF_END_PROPERTY),
							(Integer) m.get(AlignedSegment.MEMBER_START_PROPERTY),
							(Integer) m.get(AlignedSegment.MEMBER_END_PROPERTY)))
					.collect(Collectors.toList());
			return queryAlignedSegments;
		}
	}
	
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {}
	
}
