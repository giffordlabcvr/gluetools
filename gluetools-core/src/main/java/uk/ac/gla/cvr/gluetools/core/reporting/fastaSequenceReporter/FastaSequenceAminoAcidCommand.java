package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.Translator;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(
		commandWords={"amino-acid"}, 
		description = "Translate amino acids in a FASTA file", 
		docoptUsages = { "-i <fileName> -r <acRefName> -f <featureName> -t <targetRefName> -a <tipAlmtName>" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 FASTA input file",
				"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining reference",
				"-f <featureName>, --featureName <featureName>        Feature to translate",
				"-t <targetRefName>, --targetRefName <targetRefName>  Target reference",
				"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
		},
		furtherHelp = 
		        "This command aligns a FASTA query sequence to a 'target' reference sequence, and "+
		        "translates a section of the query sequence to amino acids based on the target reference sequence's "+
				"place in the alignment tree. The target reference sequence must be a member of a specified constrained 'tip alignment'. "+
		        "The <refName> argument specifies an 'ancestor-constraining' reference sequence. "+
				"This must be the constraining reference of an ancestor alignment of the tip alignment. "+
				"The <featureName> arguments specifies a feature location on the ancestor-constraining reference. "+
				"The translated amino acids will be limited to the specified feature location. ",
		metaTags = {CmdMeta.consoleOnly}	
)
public class FastaSequenceAminoAcidCommand extends ModulePluginCommand<FastaSequenceAminoAcidResult, FastaSequenceReporter> 
	implements ProvidedProjectModeCommand{

	public static final String FILE_NAME = "fileName";

	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	
	public static final String TARGET_REF_NAME = "targetRefName";
	public static final String TIP_ALMT_NAME = "tipAlmtName";


	private String fileName;
	private String acRefName;
	private String featureName;
	private String tipAlmtName;
	private String targetRefName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.tipAlmtName = PluginUtils.configureStringProperty(configElem, TIP_ALMT_NAME, true);
		this.targetRefName = PluginUtils.configureStringProperty(configElem, TARGET_REF_NAME, true);

		super.configure(pluginConfigContext, configElem);
	}

	@Override
	protected FastaSequenceAminoAcidResult execute(CommandContext cmdContext,
			FastaSequenceReporter fastaSequenceReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		
		Entry<String, DNASequence> fastaEntry = getFastaEntry(consoleCmdContext);
		String fastaID = fastaEntry.getKey();
		DNASequence fastaNTSeq = fastaEntry.getValue();

		Alignment tipAlmt = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(tipAlmtName));

		// check that tip alignment is constrained
		tipAlmt.getConstrainingRef();

		ReferenceSequence ancConstrainingRef = tipAlmt.getAncConstrainingRef(cmdContext, acRefName);
		Alignment ancestorAlignment = tipAlmt.getAncestorWithReferenceName(acRefName);
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName), false);

		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));

		AlignmentMember tipAlmtMember = getTipAlmtMember(cmdContext, targetRef, tipAlmt);

		Feature feature = featureLoc.getFeature();
		feature.checkCodesAminoAcids();

		// align query to target reference
		Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, fastaSequenceReporter.getAlignerModuleName());
		Map<String, DNASequence> fastaIDToSequence = new LinkedHashMap<String, DNASequence>();
		fastaIDToSequence.put(fastaID, fastaNTSeq);
		AlignerResult alignerResult = aligner.doAlign(cmdContext, targetRef.getName(), fastaIDToSequence);
		
		// extract segments from aligner result
		List<QueryAlignedSegment> queryToTargetRefSegs = alignerResult.getQueryIdToAlignedSegments().get(fastaID);

		// translate segments to tip alignment reference
		List<QueryAlignedSegment> queryToTipAlmtRefSegs = tipAlmt.translateToRef(cmdContext, 
				tipAlmtMember.getSequence().getSource().getName(), tipAlmtMember.getSequence().getSequenceID(), 
				queryToTargetRefSegs);
		
		// translate segments to ancestor constraining reference
		List<QueryAlignedSegment> queryToAncConstrRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, queryToTipAlmtRefSegs, ancConstrainingRef);


		// trim down to the feature area.
		List<ReferenceSegment> featureRefSegs = featureLoc.getSegments().stream()
				.map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());
		List<QueryAlignedSegment> queryToAncConstrRefSegs = 
					ReferenceSegment.intersection(queryToAncConstrRefSegsFull, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
			
		// truncate to codon aligned
		Integer codon1Start = featureLoc.getCodon1Start(cmdContext);

		List<QueryAlignedSegment> queryToAncConstrRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, queryToAncConstrRefSegs);

		final Translator translator = new CommandContextTranslator(cmdContext);

		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		
		int ntStart = Integer.MAX_VALUE;
		int ntEnd = Integer.MIN_VALUE;
		
		String fastaNTs = fastaNTSeq.getSequenceAsString();
		for(QueryAlignedSegment queryToGlueRefSeg: queryToAncConstrRefSegsCodonAligned) {
			CharSequence nts = SegmentUtils.base1SubString(fastaNTs, queryToGlueRefSeg.getQueryStart(), queryToGlueRefSeg.getQueryEnd());
			String segAAs = translator.translate(nts);
			int nt = queryToGlueRefSeg.getRefStart();
			for(int i = 0; i < segAAs.length(); i++) {
				String segAA = segAAs.substring(i,  i+1);
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				row.put(FastaSequenceAminoAcidResult.AMINO_ACID, segAA);
				row.put(FastaSequenceAminoAcidResult.NT_START, nt);
				rowData.add(row);
				ntStart = Math.min(ntStart, nt);
				ntEnd = Math.max(ntEnd, nt+2);
				nt = nt+3;
			}
		}

		List<LabeledCodon> labeledCodons = ancestorAlignment.labelCodons(cmdContext, featureName, ntStart, ntEnd);
		TIntObjectMap<LabeledCodon> ntStartToLabeled = new TIntObjectHashMap<LabeledCodon>();
		for(LabeledCodon labeledCodon: labeledCodons) {
			ntStartToLabeled.put(labeledCodon.getNtStart(), labeledCodon);
		}
		
		for(Map<String, Object> row: rowData) {
			Integer rowNtStart = (Integer) row.get(FastaSequenceAminoAcidResult.NT_START);
			LabeledCodon labeledCodon = ntStartToLabeled.get(rowNtStart);
			row.put(FastaSequenceAminoAcidResult.CODON, labeledCodon == null ? null : labeledCodon.getLabel());
		}
		
		return new FastaSequenceAminoAcidResult(rowData);
		
	}

	private AlignmentMember getTipAlmtMember(CommandContext cmdContext, ReferenceSequence targetRef, Alignment tipAlignment) {
		Sequence targetRefSeq = targetRef.getSequence();
		return GlueDataObject.lookup(cmdContext, 
				AlignmentMember.class, 
				AlignmentMember.pkMap(tipAlignment.getName(), targetRefSeq.getSource().getName(), targetRefSeq.getSequenceID()));
		
	}

	private Entry<String, DNASequence> getFastaEntry(
			ConsoleCommandContext consoleCmdContext) {
		byte[] fastaFileBytes = consoleCmdContext.loadBytes(fileName);
		FastaUtils.normalizeFastaBytes(consoleCmdContext, fastaFileBytes);
		Map<String, DNASequence> headerToSeq = FastaUtils.parseFasta(fastaFileBytes);
		if(headerToSeq.size() > 1) {
			throw new FastaSequenceException(Code.MULTIPLE_FASTA_FILE_SEQUENCES, fileName);
		}
		if(headerToSeq.size() == 0) {
			throw new FastaSequenceException(Code.NO_FASTA_FILE_SEQUENCES, fileName);
		}
		Entry<String, DNASequence> singleEntry = headerToSeq.entrySet().iterator().next();
		return singleEntry;
	}


	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
			registerDataObjectNameLookup("acRefName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String acRefName = (String) bindings.get("acRefName");
					if(acRefName != null) {
						ReferenceSequence acRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(acRefName), true);
						if(acRef != null) {
							return acRef.getFeatureLocations().stream()
									.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true))
									.collect(Collectors.toList());
						}
					}
					return null;
				}
			});
			registerDataObjectNameLookup("targetRefName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
			registerVariableInstantiator("tipAlmtName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String targetRefName = (String) bindings.get("targetRefName");
					if(targetRefName != null) {
						ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName), true);
						if(targetRef != null) {
							Sequence targetRefSeq = targetRef.getSequence();
							Expression exp = ExpressionFactory.matchExp(AlignmentMember.SEQUENCE_ID_PATH, targetRefSeq.getSequenceID());
							exp = exp.andExp(ExpressionFactory.matchExp(AlignmentMember.SOURCE_NAME_PATH, targetRefSeq.getSource().getName()));
							List<AlignmentMember> almtMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, new SelectQuery(AlignmentMember.class, exp));
							return almtMembers.stream().map(am -> new CompletionSuggestion(am.getAlignment().getName(), true)).collect(Collectors.toList());
						}
					}
					return null;
				}
			});
		}
		
	}

	
	
}
