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
package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@CommandClass(
		commandWords={"translate", "segments"}, 
		description = "Translate segments between references", 
		docoptUsages = {}, 
		metaTags={CmdMeta.inputIsComplex},
		furtherHelp = "Given a set of segments relating a query sequence Q to a reference R1, "+
				"and another set of segments relating R1 to a reference R2, produce a set of segments "+
				"relating Q to R2. \n"+
				"Example JSON input:\n"+
				"{\n"+
				"  translate: {\n"+
				"  {\n"+
				"    segments: {\n"+
				"      queryToRef1Segment: [\n"+
				"        {\n"+
				"          refStart: 49,\n"+
				"          refEnd: 90,\n"+
				"          queryStart: 29,\n"+
				"          queryEnd: 70\n"+
				"        },\n"+
				"        {\n"+
				"          refStart: 104,\n"+
				"          refEnd: 155,\n"+
				"          queryStart: 94,\n"+
				"          queryEnd: 145\n"+
				"        } ],\n"+
				"      ref1ToRef2Segment: [\n"+
				"        {\n"+
				"          refStart: 49,\n"+
				"          refEnd: 70,\n"+
				"          queryStart: 19,\n"+
				"          queryEnd: 40\n"+
				"        },\n"+
				"        {\n"+
				"          refStart: 104,\n"+
				"          refEnd: 155,\n"+
				"          queryStart: 54,\n"+
				"          queryEnd: 105\n"+
				"        } ]\n"+
				"      }\n"+
				"    }\n"+
				"  }\n"+
				"}"
)
public class TranslateSegmentsCommand extends Command<TranslateSegmentsCommand.TranslateSegmentsResult> {


	public static final String QUERY_TO_REF1_SEGMENT = "queryToRef1Segment";
	public static final String REF1_TO_REF2_SEGMENT = "ref1ToRef2Segment";
	
	private LinkedList<QueryAlignedSegment> queryToRef1Segments;
	private LinkedList<QueryAlignedSegment> ref1ToRef2Segments;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		List<Element> queryToRef1SegmentElems = PluginUtils.findConfigElements(configElem, QUERY_TO_REF1_SEGMENT);
		queryToRef1Segments = new LinkedList<QueryAlignedSegment>(queryToRef1SegmentElems.stream()
				.map(elem -> new QueryAlignedSegment(pluginConfigContext, elem))
				.collect(Collectors.toList()));
		List<Element> ref1ToRef2SegmentElems = PluginUtils.findConfigElements(configElem, REF1_TO_REF2_SEGMENT);
		ref1ToRef2Segments = new LinkedList<QueryAlignedSegment>(ref1ToRef2SegmentElems.stream()
				.map(elem -> new QueryAlignedSegment(pluginConfigContext, elem))
				.collect(Collectors.toList()));
	}

	@Override
	public TranslateSegmentsResult execute(CommandContext cmdContext) {
		return new TranslateSegmentsResult(QueryAlignedSegment.translateSegments(queryToRef1Segments, ref1ToRef2Segments));
	}
	
	public static class TranslateSegmentsResult extends CommandResult {

		protected TranslateSegmentsResult(List<QueryAlignedSegment> resultSegments) {
			super("translateSegmentsResult");
			CommandArray resultSegmentArrayBuilder = getCommandDocument().setArray("queryToRef2Segments");
			for(QueryAlignedSegment resultSegment: resultSegments) {
				resultSegment.toDocument(resultSegmentArrayBuilder.addObject());
			}
		}
		
		public List<QueryAlignedSegment> getResultSegments() {
			CommandArray queryToRef2SegmentsArray = getCommandDocument().getArray("queryToRef2Segments");
			List<QueryAlignedSegment> resultSegments = new ArrayList<QueryAlignedSegment>();
			for(int i = 0; i < queryToRef2SegmentsArray.size(); i++) {
				resultSegments.add(new QueryAlignedSegment(queryToRef2SegmentsArray.getObject(i)));
			}
			return resultSegments;
		}

	}

}
