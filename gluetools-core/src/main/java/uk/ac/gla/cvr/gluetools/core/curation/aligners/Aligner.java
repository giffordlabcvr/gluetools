package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ArrayReader;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class Aligner<R extends Aligner.AlignerResult, P extends ModulePlugin<P>> extends ModulePlugin<P> {

	
	public static final String ALIGN_COMMAND_WORD = "align";
	public static final String ALIGN_COMMAND_DOCOPT_USAGE = "<referenceName> <queryFormat> <queryBase64>";
	public static final String ALIGN_COMMAND_DOCOPT_OPTIONS = "";
	public static final String ALIGN_COMMAND_FURTHER_HELP = 
			"The <referenceFormat> and <queryFormat> arguments specify the data format of the reference and query sequences, "+
			"the sequence data bytes are supplied in Base64 binary encoding as the <referenceBase64> and <queryBase64> arguments.";
	
	public static abstract class AlignCommand<R extends Aligner.AlignerResult, P extends ModulePlugin<P>> 
		extends ModuleProvidedCommand<R, P> implements ProvidedProjectModeCommand {

		public static final String REFERENCE_NAME = "referenceName";
		public static final String QUERY_FORMAT = "queryFormat";
		public static final String QUERY_BASE64 = "queryBase64";

		private String referenceName;
		private SequenceFormat querySequenceFormat;
		private byte[] querySequenceBytes;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
			querySequenceFormat = PluginUtils.configureEnumProperty(SequenceFormat.class, configElem, QUERY_FORMAT, true);
			querySequenceBytes= PluginUtils.configureBase64BytesProperty(configElem, QUERY_BASE64, true);
		}
	
		protected byte[] getQuerySequenceBytes() {
			return querySequenceBytes;
		}
		
		protected SequenceFormat getQuerySequenceFormat() {
			return querySequenceFormat;
		}
		
		protected String getReferenceName() {
			return referenceName;
		}

	}
	@SuppressWarnings("rawtypes")
	public abstract Class<? extends Aligner.AlignCommand> getAlignCommandClass();
	
	public abstract static class AlignerResult extends CommandResult {

		public static class AlignedSegment {
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
			ArrayReader alignedSegmentsReader = 
					getDocumentReader().getArray("alignedSegment");
			List<AlignedSegment> segments = new ArrayList<AlignedSegment>();
			for(int i = 0; i < alignedSegmentsReader.size(); i++) {
				ObjectReader objectReader = alignedSegmentsReader.getObject(i);
				segments.add(new AlignedSegment(
						objectReader.intValue("refStart"),
						objectReader.intValue("refEnd"),
						objectReader.intValue("queryStart"),
						objectReader.intValue("queryEnd")));

			}
			return segments;
		}
	}



}
