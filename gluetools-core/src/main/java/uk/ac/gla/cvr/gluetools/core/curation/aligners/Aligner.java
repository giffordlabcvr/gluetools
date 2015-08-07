package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

public abstract class Aligner<R extends Aligner.AlignerResult, P extends ModulePlugin<P>> extends ModulePlugin<P> {

	
	public static final String ALIGN_COMMAND_WORD = "align";
	public static final String ALIGN_COMMAND_DOCOPT_USAGE = "<referenceFormat> <referenceBase64> <queryFormat> <queryBase64>";
	public static final String ALIGN_COMMAND_DOCOPT_OPTIONS = "";
	public static final String ALIGN_COMMAND_FURTHER_HELP = 
			"The <referenceFormat> and <queryFormat> arguments specify the data format of the reference and query sequences, "+
			"the sequence data bytes are supplied in Base64 binary encoding as the <referenceBase64> and <queryBase64> arguments.";
	
	public abstract class AlignCommand extends ModuleProvidedCommand<R, P> implements ProvidedProjectModeCommand {

		public static final String REFERENCE_FORMAT = "referenceFormat";
		public static final String REFERENCE_BASE64 = "referenceBase64";
		public static final String QUERY_FORMAT = "queryFormat";
		public static final String QUERY_BASE64 = "queryBase64";

		private SequenceFormat referenceSequenceFormat;
		private byte[] referenceSequenceBytes;
		private SequenceFormat querySequenceFormat;
		private byte[] querySequenceBytes;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			referenceSequenceFormat = PluginUtils.configureEnumProperty(SequenceFormat.class, configElem, REFERENCE_FORMAT, true);
			referenceSequenceBytes= PluginUtils.configureBase64BytesProperty(configElem, REFERENCE_BASE64, true);
			querySequenceFormat = PluginUtils.configureEnumProperty(SequenceFormat.class, configElem, QUERY_FORMAT, true);
			querySequenceBytes= PluginUtils.configureBase64BytesProperty(configElem, QUERY_BASE64, true);
		}
	
		protected byte[] getQuerySequenceBytes() {
			return querySequenceBytes;
		}
		
		protected SequenceFormat getQuerySequenceFormat() {
			return querySequenceFormat;
		}
		
		protected SequenceFormat getReferenceSequenceFormat() {
			return referenceSequenceFormat;
		}

		protected byte[] getReferenceSequenceBytes() {
			return referenceSequenceBytes;
		}

	}
	public abstract Class<? extends Aligner<?, ?>.AlignCommand> getAlignCommandClass();
	
	public abstract static class AlignerResult extends CommandResult {

		public class AlignedSegment {
			private int refStart, refEnd, queryStart, queryEnd;

			public AlignedSegment(int refStart, int refEnd, int queryStart,
					int queryEnd) {
				super();
				this.refStart = refStart;
				this.refEnd = refEnd;
				this.queryStart = queryStart;
				this.queryEnd = queryEnd;
			}
			public int getRefStart() { return refStart; }
			public int getRefEnd() { return refEnd; }
			public int getQueryStart() { return queryStart; }
			public int getQueryEnd() { return queryEnd; }
		}

		protected AlignerResult(String rootObjectName, List<AlignedSegment> alignedSegments) {
			super(rootObjectName);
			ArrayBuilder arrayBuilder = getDocumentBuilder().setArray("alignedSegment");
			for(AlignedSegment segment: alignedSegments) {
				arrayBuilder.addObject()
					.set("refStart", segment.getRefStart())
					.set("refEnd", segment.getRefEnd())
					.set("queryStart", segment.getQueryStart())
					.set("queryEnd", segment.getQueryEnd());
			}
		}

		public List<AlignedSegment> getAlignedSegments() {
			List<Element> alignedSegmentElems = 
					GlueXmlUtils.findChildElements(getDocument().getDocumentElement(), "alignedSegment");
			return alignedSegmentElems.stream().map(elem -> 
				new AlignedSegment(
						// boilerplate hell! need to sort this out at some point!
						(Integer) JsonUtils.elementToObject(GlueXmlUtils.findChildElements(elem, "refStart").get(0)),
						(Integer) JsonUtils.elementToObject(GlueXmlUtils.findChildElements(elem, "refEnd").get(0)),
						(Integer) JsonUtils.elementToObject(GlueXmlUtils.findChildElements(elem, "queryStart").get(0)),
						(Integer) JsonUtils.elementToObject(GlueXmlUtils.findChildElements(elem, "queryEnd").get(0)))
			).collect(Collectors.toList());
		}
		
	}


	
	
}
