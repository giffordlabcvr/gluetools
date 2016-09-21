package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
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
		extends ModulePluginCommand<R, P> implements ProvidedProjectModeCommand {

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
	extends ModulePluginCommand<R, P> implements ProvidedProjectModeCommand {

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
		FastaUtils.normalizeFastaBytes(consoleCmdContext, sequenceFileBytes);
		return FastaUtils.parseFasta(sequenceFileBytes);
	}
	
	protected String getReferenceName() {
		return referenceName;
	}

}

	
	
	public abstract static class AlignerResult extends CommandResult {

		protected AlignerResult(String rootObjectName, Map<String, List<QueryAlignedSegment>> queryIdToAlignedSegments) {
			super(rootObjectName);
			CommandArray sequenceArrayBuilder = getCommandDocument().setArray("sequence");
			queryIdToAlignedSegments.forEach((queryId, alignedSegments) -> {
				CommandObject sequenceObjectBuilder = sequenceArrayBuilder.addObject();
				sequenceObjectBuilder.set("queryId", queryId);
				CommandArray alignedSegmentArrayBuilder = sequenceObjectBuilder.setArray("alignedSegment");
				for(QueryAlignedSegment segment: alignedSegments) {
					CommandObject alignedSegmentObjectBuilder = alignedSegmentArrayBuilder.addObject();
					segment.toDocument(alignedSegmentObjectBuilder);
				}
			});
		}

		public Map<String, List<QueryAlignedSegment>> getQueryIdToAlignedSegments() {
			Map<String, List<QueryAlignedSegment>> queryIdToAlignedSegments = 
					new LinkedHashMap<String, List<QueryAlignedSegment>>();
			CommandArray sequencesArray = getCommandDocument().getArray("sequence");
			for(int i = 0 ; i < sequencesArray.size(); i++) {
				CommandObject sequenceCommandObject = sequencesArray.getObject(i);
				String queryId = sequenceCommandObject.getString("queryId");
				CommandArray alignedSegmentsArray = 
						sequenceCommandObject.getArray("alignedSegment");
				List<QueryAlignedSegment> segments = new ArrayList<QueryAlignedSegment>();
				for(int j = 0; j < alignedSegmentsArray.size(); j++) {
					CommandObject alignedSegmentsObject = alignedSegmentsArray.getObject(j);
					segments.add(new QueryAlignedSegment(alignedSegmentsObject));

				}
				queryIdToAlignedSegments.put(queryId, segments);
			}
			return queryIdToAlignedSegments;
		}
	}

	public static Aligner<?, ?> getAligner(CommandContext cmdContext,
			String alignerModuleName) {
		return Module.resolveModulePlugin(cmdContext, Aligner.class, alignerModuleName);
	}

	public final R computeConstrained(CommandContext cmdContext, String refName, String queryId, DNASequence nucleotides) {
		Map<String, DNASequence> queryIdToNucleotides = new LinkedHashMap<String, DNASequence>();
		queryIdToNucleotides.put(queryId, nucleotides);
		return computeConstrained(cmdContext, refName, queryIdToNucleotides);
	}
	
	public R computeConstrained(CommandContext cmdContext, String refName, Map<String, DNASequence> queryIdToNucleotides) {
		throw new RuntimeException("Compute of constrained alignments is unsupported.");
	}

	public R extendUnconstrained(CommandContext cmdContext, Map<String, DNASequence> existingIdToNucleotides, Map<String, DNASequence> queryIdToNucleotides) {
		throw new RuntimeException("Extend of unconstrained alignments is unsupported.");
	}
	
	protected Map<String, List<QueryAlignedSegment>> initFastaIdToAlignedSegments(Collection<String> queryIds) {
		Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments = new LinkedHashMap<String, List<QueryAlignedSegment>>();
		for(String queryId: queryIds) {
			fastaIdToAlignedSegments.put(queryId, new ArrayList<QueryAlignedSegment>());
		}
		return fastaIdToAlignedSegments;
	}


	
}
