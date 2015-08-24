package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ArrayReader;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"translate", "segments"}, 
		description = "Translate segments between alignments", 
		docoptUsages = {}, 
		metaTags={CmdMeta.inputIsComplex},
		furtherHelp = "Given a set of segments relating a query sequence Q to the reference R1 of a given alignment A1, "+
				"translate these segments so that they relate Q to a different reference R2, where R2 is the reference "+
				"sequence of some alignment A2 which is an ancestor of A1. \n"+
				"Example JSON input:\n"+
				"{\n"+
				"  translate: {\n"+
				"  {\n"+
				"    segments: {\n"+
				"      fromAlignmentName: \"A1\",\n"+
				"      toAlignmentName: \"A2\",\n"+
				"      queryAlignedSegment: [\n"+
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
				"        } ]\n"+
				"      }\n"+
				"    }\n"+
				"  }\n"+
				"}"
)
public class TranslateSegmentsCommand extends Command<TranslateSegmentsCommand.TransformSegmentsResult> {


	private String fromAlignmentName;
	private String toAlignmentName;
	private List<QueryAlignedSegment> inputSegments;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fromAlignmentName = PluginUtils.configureStringProperty(configElem, "fromAlignmentName", true);
		toAlignmentName = PluginUtils.configureStringProperty(configElem, "toAlignmentName", true);
		List<Element> inputSegmentElems = PluginUtils.findConfigElements(configElem, "queryAlignedSegment");
		inputSegments = inputSegmentElems.stream()
				.map(elem -> new QueryAlignedSegment(new ObjectReader(elem)))
				.collect(Collectors.toList());
	}

	@Override
	public TransformSegmentsResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Alignment fromAlignment = GlueDataObject.lookup(objContext, Alignment.class, Alignment.pkMap(fromAlignmentName));
		Alignment toAlignment = GlueDataObject.lookup(objContext, Alignment.class, Alignment.pkMap(toAlignmentName));
		
		return null;
	}

	public static class TransformSegmentsResult extends CommandResult {

		protected TransformSegmentsResult(List<QueryAlignedSegment> resultSegments) {
			super("transformSegmentsResult");
			ArrayBuilder resultSegmentArrayBuilder = getDocumentBuilder().setArray("queryAlignedSegment");
			for(QueryAlignedSegment resultSegment: resultSegments) {
				resultSegment.toDocument(resultSegmentArrayBuilder.addObject());
			}
		}
		
		public List<QueryAlignedSegment> getResultSegments() {
			ArrayReader arrayReader = getDocumentReader().getArray("queryAlignedSegment");
			List<QueryAlignedSegment> resultSegments = new ArrayList<QueryAlignedSegment>();
			for(int i = 0; i < arrayReader.size(); i++) {
				resultSegments.add(new QueryAlignedSegment(arrayReader.getObject(i)));
			}
			return resultSegments;
		}

	}

}
