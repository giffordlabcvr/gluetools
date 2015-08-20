package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ArrayReader;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public abstract class Aligner<R extends Aligner.AlignerResult, P extends ModulePlugin<P>> extends ModulePlugin<P> {

	public static final String ALIGN_COMMAND_WORD = "align";
	public static final boolean ALIGN_COMMAND_IS_INPUT_COMPLEX = true;
	public static final String ALIGN_COMMAND_FURTHER_HELP = 
		"Example JSON input:\n"+
			"{\n"+
			"  align: {\n"+
			"    referenceName: \"REF_328\",\n"+
			"    sequence: [\n"+
			"      {\n"+
			"        queryId: \"QuerySeq1\",\n"+
			"        nucleotides: \"ATCGACGCAGCGACGACGACTACGGGCGCCATCGACTACGACTAT\"\n"+
			"      },\n"+
			"      {\n"+
			"        queryId: \"QuerySeq2\",\n"+
			"        nucleotides: \"GCTGCGTGTGCAGACGAGGCTGACTAGCTAGACTAGACCCGCATC\"\n"+
			"      }\n"+
			"    ]\n"+
			"  }\n"+
			"}";
	
	public static abstract class AlignCommand<R extends Aligner.AlignerResult, P extends ModulePlugin<P>> 
		extends ModuleProvidedCommand<R, P> implements ProvidedProjectModeCommand {

		public static final String REFERENCE_NAME = "referenceName";
		public static final String SEQUENCE = "sequence";
		public static final String QUERY_ID = "queryId";
		public static final String NUCLEOTIDES = "nucleotides";

		private String referenceName;
		private Map<String,DNASequence> queryIdToNucleotides;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
			List<Element> sequenceElems = PluginUtils.findConfigElements(configElem, SEQUENCE, null, null);
			this.queryIdToNucleotides = new LinkedHashMap<String, DNASequence>();
			for(Element sequenceElem: sequenceElems) {
				String queryId = PluginUtils.configureStringProperty(sequenceElem, QUERY_ID, true);
				DNASequence nucleotides = PluginUtils.parseNucleotidesProperty(sequenceElem, NUCLEOTIDES, true);
				queryIdToNucleotides.put(queryId, nucleotides);
			}
		}
	
		protected Map<String, DNASequence> getQueryIdToNucleotides() {
			return queryIdToNucleotides;
		}
		
		protected String getReferenceName() {
			return referenceName;
		}

	}
	
	public static final String FILE_ALIGN_COMMAND_WORD = "file-align";
	public static final String FILE_ALIGN_COMMAND_DOCOPT_USAGE = 
			"<referenceName> <sequenceFileName>";
	public static final String FILE_ALIGN_COMMAND_FURTHER_HELP = 
		"The file must be in FASTA format";

	
	public static abstract class FileAlignCommand<R extends Aligner.AlignerResult, P extends ModulePlugin<P>> 
	extends ModuleProvidedCommand<R, P> implements ProvidedProjectModeCommand {

	public static final String REFERENCE_NAME = "referenceName";
	public static final String SEQUENCE_FILE_NAME = "sequenceFileName";

	private String referenceName;
	private String sequenceFileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
		sequenceFileName = PluginUtils.configureStringProperty(configElem, SEQUENCE_FILE_NAME, true);
		
	}

	protected Map<String, DNASequence> getQueryIdToNucleotides(ConsoleCommandContext consoleCmdContext) {
		byte[] sequenceFileBytes = consoleCmdContext.loadBytes(sequenceFileName);
		return FastaUtils.parseFasta(sequenceFileBytes);
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
			public int getRefStart() {
				return refStart;
			}
			public void setRefStart(int refStart) {
				this.refStart = refStart;
			}
			public int getRefEnd() {
				return refEnd;
			}
			public void setRefEnd(int refEnd) {
				this.refEnd = refEnd;
			}
			public int getQueryStart() {
				return queryStart;
			}
			public void setQueryStart(int queryStart) {
				this.queryStart = queryStart;
			}
			public int getQueryEnd() {
				return queryEnd;
			}
			public void setQueryEnd(int queryEnd) {
				this.queryEnd = queryEnd;
			}

			public String toString() { return
				"Ref: ["+getRefStart()+", "+getRefEnd()+"] "+
						"<-> Query: ["+getQueryStart()+", "+getQueryEnd()+"]";
			}
			
			/**
			 * returns true if the two segments propose the same offset between query and reference,
			 * 
			 * This is useful to know in the case where the reference ranges overlap: 
			 * in this case the segments can easily be merged.
			 */
			public boolean isAlignedTo(AlignedSegment other) {
				return queryStart - refStart == other.queryStart - other.refStart;
			}
			
			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + queryEnd;
				result = prime * result + queryStart;
				result = prime * result + refEnd;
				result = prime * result + refStart;
				return result;
			}
			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				AlignedSegment other = (AlignedSegment) obj;
				if (queryEnd != other.queryEnd)
					return false;
				if (queryStart != other.queryStart)
					return false;
				if (refEnd != other.refEnd)
					return false;
				if (refStart != other.refStart)
					return false;
				return true;
			}

			
		}

		protected AlignerResult(String rootObjectName, Map<String, List<AlignedSegment>> fastaIdToAlignedSegments) {
			super(rootObjectName);
			ArrayBuilder sequenceArrayBuilder = getDocumentBuilder().setArray("sequence");
			fastaIdToAlignedSegments.forEach((fastaId, alignedSegments) -> {
				ObjectBuilder sequenceObjectBuilder = sequenceArrayBuilder.addObject();
				sequenceObjectBuilder.set("fastaId", fastaId);
				ArrayBuilder alignedSegmentArrayBuilder = sequenceObjectBuilder.setArray("alignedSegment");
				for(AlignedSegment segment: alignedSegments) {
					alignedSegmentArrayBuilder.addObject()
					.set("refStart", segment.getRefStart())
					.set("refEnd", segment.getRefEnd())
					.set("queryStart", segment.getQueryStart())
					.set("queryEnd", segment.getQueryEnd());
				}
			});
		}

		public Map<String, List<AlignedSegment>> getFastaIdToAlignedSegments() {
			Map<String, List<AlignedSegment>> fastaIdToAlignedSegments = 
					new LinkedHashMap<String, List<AlignedSegment>>();
			ArrayReader sequencesReader = getDocumentReader().getArray("sequence");
			for(int i = 0 ; i < sequencesReader.size(); i++) {
				ObjectReader sequenceObjectReader = sequencesReader.getObject(i);
				String fastaId = sequenceObjectReader.stringValue("fastaId");
				ArrayReader alignedSegmentsReader = 
						sequenceObjectReader.getArray("alignedSegment");
				List<AlignedSegment> segments = new ArrayList<AlignedSegment>();
				for(int j = 0; j < alignedSegmentsReader.size(); j++) {
					ObjectReader objectReader = alignedSegmentsReader.getObject(j);
					segments.add(new AlignedSegment(
							objectReader.intValue("refStart"),
							objectReader.intValue("refEnd"),
							objectReader.intValue("queryStart"),
							objectReader.intValue("queryEnd")));

				}
				fastaIdToAlignedSegments.put(fastaId, segments);
			}
			return fastaIdToAlignedSegments;
		}
	}



}
