package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import gnu.trove.map.TCharIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TCharIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TCharIntProcedure;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
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
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.RecordsCounter;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.Translator;

@CommandClass(
		commandWords={"amino-acid"}, 
		description = "Translate amino acids in a SAM/BAM file", 
		docoptUsages = { "-i <fileName> [-s <samRefName>] -r <acRefName> -f <featureName> [-l] -t <targetRefName> [-a <tipAlmtName>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 SAM/BAM input file",
				"-s <samRefName>, --samRefName <samRefName>           Specific SAM ref seq",
				"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
				"-f <featureName>, --featureName <featureName>        Feature to translate",
				"-l, --autoAlign                                      Auto-align consensus",
				"-t <targetRefName>, --targetRefName <targetRefName>  Target GLUE reference",
				"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
		},
		furtherHelp = 
			"This command translates a SAM/BAM file reads to amino acids. "+
			"If <samRefName> is supplied, the translated reads are limited to those which are aligned to the "+
			"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
			"file only names a single reference sequence.\n"+
			"The translation is based on a 'target' GLUE reference sequence's place in the alignment tree. "+
			"By default, the SAM file is assumed to align reads against this target reference, i.e. the target GLUE reference "+
			"is the reference sequence  mentioned in the SAM file. "+
			"Alternatively the --autoAlign option may be used; this will generate a pairwise alignment between the SAM file "+
			"consensus and the target GLUE reference. \n"+
			"The target reference sequence must be a member of a constrained "+
			"'tip alignment'. The tip alignment may be specified by <tipAlmtName>. If unspecified, it will be "+
			"inferred from the target reference if possible. "+
			"The <acRefName> argument specifies an 'ancestor-constraining' reference sequence. "+
			"This must be the constraining reference of an ancestor alignment of the tip alignment. "+
			"The <featureName> arguments specifies a feature location on the ancestor-constraining reference. "+
			"The translated amino acids will be limited to the specified feature location. ",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamAminoAcidCommand extends ModulePluginCommand<SamAminoAcidResult, SamReporter> 
	implements ProvidedProjectModeCommand{

	public static final String FILE_NAME = "fileName";
	public static final String SAM_REF_NAME = "samRefName";

	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String AUTO_ALIGN = "autoAlign";
	
	public static final String TARGET_REF_NAME = "targetRefName";
	public static final String TIP_ALMT_NAME = "tipAlmtName";
	
	private String fileName;
	private String samRefName;
	private String acRefName;
	private String featureName;
	private boolean autoAlign;
	private String targetRefName;
	private String tipAlmtName;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.samRefName = PluginUtils.configureStringProperty(configElem, SAM_REF_NAME, false);
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.autoAlign = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, AUTO_ALIGN, false)).orElse(false);
		this.targetRefName = PluginUtils.configureStringProperty(configElem, TARGET_REF_NAME, true);
		this.tipAlmtName = PluginUtils.configureStringProperty(configElem, TIP_ALMT_NAME, false);
		super.configure(pluginConfigContext, configElem);
	}


	@Override
	protected SamAminoAcidResult execute(CommandContext cmdContext, SamReporter samReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));
		AlignmentMember tipAlmtMember = targetRef.getConstrainedAlignmentMembership(tipAlmtName);
		Alignment tipAlmt = tipAlmtMember.getAlignment();
		ReferenceSequence ancConstrainingRef = tipAlmt.getAncConstrainingRef(cmdContext, acRefName);

		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName), false);
		Feature feature = featureLoc.getFeature();
		feature.checkCodesAminoAcids();

		List<QueryAlignedSegment> samRefToTargetRefSegs;
		if(autoAlign) {
			// auto-align consensus to target ref
			Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, samReporter.getAlignerModuleName());
			Map<String, DNASequence> samConsensus = SamUtils.getSamConsensus(consoleCmdContext, samRefName, fileName, "samConsensus");
			AlignerResult alignerResult = aligner.doAlign(cmdContext, targetRef.getName(), samConsensus);
			// extract segments from aligner result
			samRefToTargetRefSegs = alignerResult.getQueryIdToAlignedSegments().get("samConsensus");
		} else {
			// sam ref is same sequence as target ref, so just a single self-mapping segment.
			int targetRefLength = targetRef.getSequence().getSequenceObject().getNucleotides(consoleCmdContext).length();
			samRefToTargetRefSegs = Arrays.asList(new QueryAlignedSegment(1, targetRefLength, 1, targetRefLength));
		}
		
		// translate segments to tip alignment reference
		List<QueryAlignedSegment> samRefToTipAlmtRefSegs = tipAlmt.translateToRef(cmdContext, 
				tipAlmtMember.getSequence().getSource().getName(), tipAlmtMember.getSequence().getSequenceID(), 
				samRefToTargetRefSegs);
		
		// translate segments to ancestor constraining reference
		List<QueryAlignedSegment> samRefToAncConstrRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, samRefToTipAlmtRefSegs, ancConstrainingRef);

		// trim down to the feature area.
		List<ReferenceSegment> featureRefSegs = featureLoc.getSegments().stream()
				.map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());
		List<QueryAlignedSegment> samRefToAncConstrRefSegs = 
					ReferenceSegment.intersection(samRefToAncConstrRefSegsFull, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
			
		// truncate to codon aligned
		Integer codon1Start = featureLoc.getCodon1Start(cmdContext);

		List<QueryAlignedSegment> samRefToAncConstrRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, samRefToAncConstrRefSegs);

		if(samRefToAncConstrRefSegsCodonAligned.isEmpty()) {
			return new SamAminoAcidResult(Collections.emptyList());
		}
		
		TIntObjectMap<LabeledCodon> ancConstrRefNtToLabeledCodon = featureLoc.getRefNtToLabeledCodon(cmdContext);

		// build a map from anc constr ref NT to AA read count.
		TIntObjectMap<AminoAcidReadCount> ancConstrRefNtToAminoAcidReadCount = new TIntObjectHashMap<AminoAcidReadCount>();
		List<Integer> mappedAncConstrRefNts = new ArrayList<Integer>();
		for(QueryAlignedSegment qaSeg: samRefToAncConstrRefSegsCodonAligned) {
			for(int ancConstrRefNt = qaSeg.getRefStart(); ancConstrRefNt <= qaSeg.getRefEnd(); ancConstrRefNt++) {
				if(TranslationUtils.isAtStartOfCodon(codon1Start, ancConstrRefNt)) {
					mappedAncConstrRefNts.add(ancConstrRefNt);
					LabeledCodon labeledCodon = ancConstrRefNtToLabeledCodon.get(ancConstrRefNt);
					int samRefNt = ancConstrRefNt + qaSeg.getReferenceToQueryOffset();
					ancConstrRefNtToAminoAcidReadCount.put(ancConstrRefNt, new AminoAcidReadCount(labeledCodon, samRefNt));
				}
			}
		}
		
		// translate reads.
		final Translator translator = new CommandContextTranslator(cmdContext);
		
		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, fileName)) {
			
			SamRecordFilter samRecordFilter = new SamUtils.ReferenceBasedRecordFilter(samReader, fileName, samRefName);

	        final RecordsCounter recordsCounter = samReporter.new RecordsCounter();
			
			samReader.forEach(samRecord -> {
				if(!samRecordFilter.recordPasses(samRecord)) {
					return;
				}
				List<QueryAlignedSegment> readToSamRefSegs = getReadToSamRefSegs(samRecord);
				List<QueryAlignedSegment> readToAncConstrRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, samRefToAncConstrRefSegs);
				
				List<QueryAlignedSegment> readToAncConstrRefSegsCodonAligned = TranslationUtils.truncateToCodonAligned(codon1Start, readToAncConstrRefSegs);

				final String readString = samRecord.getReadString().toUpperCase();

				for(QueryAlignedSegment readToAncConstRefSeg: readToAncConstrRefSegsCodonAligned) {
					CharSequence nts = SegmentUtils.base1SubString(readString, readToAncConstRefSeg.getQueryStart(), readToAncConstRefSeg.getQueryEnd());
					String segAAs = translator.translate(nts);
					Integer ancConstrRefNt = readToAncConstRefSeg.getRefStart();
					for(int i = 0; i < segAAs.length(); i++) {
						char segAA = segAAs.charAt(i);
						ancConstrRefNtToAminoAcidReadCount.get(ancConstrRefNt).addAaRead(segAA);
						ancConstrRefNt += 3;
					}
				}
				recordsCounter.processedRecord();
				recordsCounter.logRecordsProcessed();
			});
			recordsCounter.logTotalRecordsProcessed();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		List<LabeledAminoAcidReadCount> rowData = new ArrayList<LabeledAminoAcidReadCount>();
		
		for(Integer ancConstrRefNt: mappedAncConstrRefNts) {
			AminoAcidReadCount aminoAcidReadCount = ancConstrRefNtToAminoAcidReadCount.get(ancConstrRefNt);
			aminoAcidReadCount.aaToReadCount.forEachEntry(new TCharIntProcedure() {
				@Override
				public boolean execute(char aminoAcid, int numReads) {
					double percentReadsWithAminoAcid = 100.0 * numReads / (double) aminoAcidReadCount.totalReadsAtCodon;
					rowData.add(new LabeledAminoAcidReadCount(
							new LabeledAminoAcid(aminoAcidReadCount.labeledCodon, 
							new String(new char[]{aminoAcid})),
							aminoAcidReadCount.samRefNt, numReads, percentReadsWithAminoAcid));
					return true;
				}
			});
		}
		
		return new SamAminoAcidResult(rowData);
		
	}

	private List<QueryAlignedSegment> getReadToSamRefSegs(SAMRecord samRecord) {
		List<QueryAlignedSegment> readToSamRefSegs = new ArrayList<QueryAlignedSegment>();
		samRecord.getAlignmentBlocks().forEach(almtBlock -> {
			int samRefStart = almtBlock.getReferenceStart();
			int samRefEnd = samRefStart + almtBlock.getLength()-1;
			int readStart = almtBlock.getReadStart();
			int readEnd = readStart + almtBlock.getLength()-1;
			readToSamRefSegs.add(new QueryAlignedSegment(samRefStart, samRefEnd, readStart, readEnd));
		});
		return readToSamRefSegs;
	}

	private class AminoAcidReadCount {
		private LabeledCodon labeledCodon;
		private int samRefNt;
		
		public AminoAcidReadCount(LabeledCodon labeledCodon, int samRefNt) {
			super();
			this.labeledCodon = labeledCodon;
			this.samRefNt = samRefNt;
		}
		private int totalReadsAtCodon = 0;
		TCharIntMap aaToReadCount = new TCharIntHashMap();
		public void addAaRead(char aaChar) {
			aaToReadCount.adjustOrPutValue(aaChar, 1, 1);
			totalReadsAtCodon++;
		}
	}

	
	@CompleterClass
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {}




	
}
