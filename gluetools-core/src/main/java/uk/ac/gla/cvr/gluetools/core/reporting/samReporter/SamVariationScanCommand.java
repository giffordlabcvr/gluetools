package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import htsjdk.samtools.SamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

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
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.RecordsCounter;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationFormat;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;

@CommandClass(
		commandWords={"variation-scan"}, 
		description = "Scan a SAM/BAM file for variations", 
		docoptUsages = { "-i <fileName> [-s <samRefName>] -r <acRefName> -f <featureName> [-l] -t <targetRefName> [-a <tipAlmtName>] [-w <whereClause>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 SAM/BAM input file",
				"-s <samRefName>, --samRefName <samRefName>           Specific SAM ref seq",
				"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
				"-f <featureName>, --featureName <featureName>        Feature to translate",
				"-l, --autoAlign                                      Auto-align consensus",
				"-t <targetRefName>, --targetRefName <targetRefName>  Target GLUE reference",
				"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
				"-w <whereClause>, --whereClause <whereClause>        Qualify variations"
		},
		furtherHelp = 
			"This command scans a SAM/BAM file for variations. "+
			"If <samRefName> is supplied, the scan limited to those reads which are aligned to the "+
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
			"The variation scan will be limited to the specified feature location. "+
			"The <whereClause> may be used to qualify which variations are scanned for. ",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamVariationScanCommand extends ModulePluginCommand<SamVariationScanResult, SamReporter> 
	implements ProvidedProjectModeCommand{

	
	public static final String FILE_NAME = "fileName";
	public static final String SAM_REF_NAME = "samRefName";

	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	public static final String AUTO_ALIGN = "autoAlign";
	
	public static final String TARGET_REF_NAME = "targetRefName";
	public static final String TIP_ALMT_NAME = "tipAlmtName";

	public static final String WHERE_CLAUSE = "whereClause";
	
	private String fileName;
	private String samRefName;
	private String acRefName;
	private String featureName;
	private boolean autoAlign;
	private String targetRefName;
	private String tipAlmtName;
	private Expression whereClause;

	
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
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		super.configure(pluginConfigContext, configElem);
	}


	@Override
	protected SamVariationScanResult execute(CommandContext cmdContext, SamReporter samReporter) {
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

        Map<String, VariationInfo> variationNameToInfo = new LinkedHashMap<String, VariationInfo>();

		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, fileName)) {
			
			SamRecordFilter samRecordFilter = new SamUtils.ReferenceBasedRecordFilter(samReader, fileName, samRefName);

	        final RecordsCounter recordsCounter = samReporter.new RecordsCounter();
			
	        
	        List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, whereClause);
	        
	        Integer codon1Start = null;
	        if(featureLoc.getFeature().codesAminoAcids()) {
	        	codon1Start = featureLoc.getCodon1Start(cmdContext);
	        }
	        
	        TIntObjectMap<List<Variation>> refNtToVariations = new TIntObjectHashMap<List<Variation>>();
	        for(Variation variation: variationsToScan) {
	        	for(int i = variation.getRefStart(); i <= variation.getRefEnd(); i++) {
	        		if(variation.getTranslationFormat() == TranslationFormat.AMINO_ACID && 
	        				!TranslationUtils.isAtStartOfCodon(codon1Start, i)) {
	        			continue;
	        		}
	        		List<Variation> variations = refNtToVariations.get(i);
	        		if(variations == null) {
	        			variations = new LinkedList<Variation>();
	        			refNtToVariations.put(i,  variations);
	        		}
	        		variations.add(variation);
	        	}
	        }
	        
			samReader.forEach(samRecord -> {
				if(!samRecordFilter.recordPasses(samRecord)) {
					return;
				}
				List<QueryAlignedSegment> readToSamRefSegs = samReporter.getReadToSamRefSegs(samRecord);
				List<QueryAlignedSegment> readToAncConstrRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, samRefToAncConstrRefSegs);
				
				final String readString = samRecord.getReadString().toUpperCase();

				List<NtQueryAlignedSegment> readToAncConstrRefNtSegs =
						readToAncConstrRefSegs.stream()
						.map(seg -> new NtQueryAlignedSegment(seg.getRefStart(), seg.getRefEnd(), seg.getQueryStart(), seg.getQueryEnd(),
								SegmentUtils.base1SubString(readString, seg.getQueryStart(), seg.getQueryEnd())))
						.collect(Collectors.toList());
				
				List<NtQueryAlignedSegment> readToAncConstrRefNtSegsMerged = 
						ReferenceSegment.mergeAbutting(readToAncConstrRefNtSegs, NtQueryAlignedSegment.mergeAbuttingFunction());
				
				List<VariationScanResult> variationScanResults = new ArrayList<VariationScanResult>();
				
				
				for(NtQueryAlignedSegment readToAncConstrRefNtSeg: readToAncConstrRefNtSegsMerged) {
					Set<Variation> variationsToScanForSegment = new LinkedHashSet<Variation>();
					for(int i = readToAncConstrRefNtSeg.getRefStart(); i <= readToAncConstrRefNtSeg.getRefEnd(); i++) {
						List<Variation> variationsAtRefNt = refNtToVariations.get(i);
						if(variationsAtRefNt != null) {
							variationsToScanForSegment.addAll(variationsAtRefNt);
						}
					}
					variationScanResults.addAll(featureLoc.variationScanSegment(cmdContext, readToAncConstrRefNtSeg, variationsToScanForSegment));
				}
			
				for(VariationScanResult variationScanResult: variationScanResults) {
					Variation variation = variationScanResult.getVariation();
					VariationInfo variationInfo = variationNameToInfo.get(variation.getName());
					if(variationInfo == null) {
						variationInfo = new VariationInfo(variation);
						variationNameToInfo.put(variation.getName(), variationInfo);
					}
					if(variationScanResult.isPresent()) {
						variationInfo.readsConfirmedPresent++;
					} else if(variationScanResult.isAbsent()) {
						variationInfo.readsConfirmedAbsent++;
					} 
				}
				
				recordsCounter.processedRecord();
				recordsCounter.logRecordsProcessed();
			});
			recordsCounter.logTotalRecordsProcessed();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		List<VariationInfo> variationInfos = new ArrayList<VariationInfo>(variationNameToInfo.values());
		
		Comparator<VariationInfo> comparator = new Comparator<VariationInfo>(){
			@Override
			public int compare(VariationInfo o1, VariationInfo o2) {
				int refStartCpResult = Integer.compare(o1.variation.getRefStart(), o2.variation.getRefStart());
				if(refStartCpResult != 0) {
					return refStartCpResult;
				}
				return o1.variation.getName().compareTo(o2.variation.getName());
			}
		};
		
		Collections.sort(variationInfos, comparator);
		
		List<VariationScanReadCount> rowData = 
				variationInfos.stream()
				.map(vInfo -> {
					int readsWherePresent = vInfo.readsConfirmedPresent;
					int readsWhereAbsent = vInfo.readsConfirmedAbsent;
					double pctWherePresent = 100.0 * readsWherePresent / (readsWherePresent + readsWhereAbsent);
					double pctWhereAbsent = 100.0 * readsWhereAbsent / (readsWherePresent + readsWhereAbsent);
					return new VariationScanReadCount(vInfo.variation.getName(), 
							readsWherePresent, pctWherePresent, 
							readsWhereAbsent, pctWhereAbsent);
				})
				.collect(Collectors.toList());
		
		return new SamVariationScanResult(rowData);
		
	}
	
	private class VariationInfo {
		Variation variation;
		int readsConfirmedPresent = 0;
		int readsConfirmedAbsent = 0;
		public VariationInfo(Variation variation) {
			super();
			this.variation = variation;
		}
	}
	
	@CompleterClass
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {}






	
}
