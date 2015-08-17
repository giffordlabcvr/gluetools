package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

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

public abstract class Aligner<R extends Aligner.AlignerResult, P extends ModulePlugin<P>> extends ModulePlugin<P> {

	
	public static final String ALIGN_COMMAND_WORD = "align";
	public static final String ALIGN_COMMAND_DOCOPT_USAGE = "-r <referenceName> -q <queryFasta>";
	public static final String ALIGN_COMMAND_DOCOPT_OPTION1 = 
		"-r, --referenceName  Reference sequence name";
	public static final String ALIGN_COMMAND_DOCOPT_OPTION2 = 
		"-q, --queryFasta  Query sequence FASTA data";
	public static final String ALIGN_COMMAND_FURTHER_HELP = 
			"The <referenceName> argument specifies the name of a reference sequence, "+
			"the sequence data is supplied in FASTA as <queryFasta>.";
	
	public static abstract class AlignCommand<R extends Aligner.AlignerResult, P extends ModulePlugin<P>> 
		extends ModuleProvidedCommand<R, P> implements ProvidedProjectModeCommand {

		public static final String REFERENCE_NAME = "referenceName";
		public static final String QUERY_FASTA = "queryFasta";

		private String referenceName;
		private String queryFasta;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
			queryFasta = PluginUtils.configureStringProperty(configElem, QUERY_FASTA, true);
		}
	
		protected String getQueryFasta() {
			return queryFasta;
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
