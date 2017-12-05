package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.SamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.RecordsCounter;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegmentTree;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a SAM/BAM file for variations", 
		docoptUsages = { "-i <fileName> [-n <samRefSense>] [-s <samRefName>] -r <acRefName> [-m] -f <featureName> [-d] (-p | [-l][-t <targetRefName>] [-a <tipAlmtName>] ) [-w <whereClause>] [-q <minQScore>] [-e <minDepth>] [-P <minPresentPct>] [-A <minAbsentPct>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 SAM/BAM input file",
				"-n <samRefSense>, --samRefSense <samRefSense>        SAM ref seq sense",
				"-s <samRefName>, --samRefName <samRefName>           Specific SAM ref seq",
				"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
				"-m, --multiReference                                 Scan across references",
				"-f <featureName>, --featureName <featureName>        Feature to translate",
				"-d, --descendentFeatures                             Include descendent features",
				"-p, --maxLikelihoodPlacer                            Use ML placer module",
				"-l, --autoAlign                                      Auto-align consensus",
				"-t <targetRefName>, --targetRefName <targetRefName>  Target GLUE reference",
				"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
				"-w <whereClause>, --whereClause <whereClause>        Qualify variations",
				"-q <minQScore>, --minQScore <minQScore>              Minimum Phred quality score",
				"-e <minDepth>, --minDepth <minDepth>                 Minimum depth",
				"-P <minPresentPct>, --minPresentPct <minPresentPct>  Show present at minimum percentage",
				"-A <minAbsentPct>, --minAbsentPct <minAbsentPct>     Show absent at minimum percentage",
		},
		furtherHelp = 
			"This command scans a SAM/BAM file for variations. "+
			"If <samRefName> is supplied, the scan limited to those reads which are aligned to the "+
			"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
			"file only names a single reference sequence.\n"+
			"The translation is based on a 'target' GLUE reference sequence's place in the alignment tree. "+
			"The <samRefSense> may be FORWARD or REVERSE_COMPLEMENT, indicating the presumed sense of the SAM reference, relative to the GLUE references."+
			"If the --maxLikelihoodPlacer option is used, an ML placement is performed, and the target reference is "+
			"identified as the closest according to this placement. "+
			"The target reference may alternatively be specified using <targetRefName>."+
			"Or, inferred from the SAM reference name, if <targetRefName> is not supplied and the module is appropriately configured. "+
			"By default, the SAM file is assumed to align reads against this target reference, i.e. the target GLUE reference "+
			"is the reference sequence  mentioned in the SAM file. "+
			"Alternatively the --autoAlign option may be used; this will generate a pairwise alignment between the SAM file "+
			"consensus and the target GLUE reference. \n"+
			"The --autoAlign option is implicit if --maxLikelihoodPlacer is used. "+
			"The target reference sequence must be a member of a constrained "+
			"'tip alignment'. The tip alignment may be specified by <tipAlmtName>. If unspecified, it will be "+
			"inferred from the target reference if possible. "+
			"The <acRefName> argument specifies an 'ancestor-constraining' reference sequence. "+
			"This must be the constraining reference of an ancestor alignment of the tip alignment. "+
			"If --multiReference is used, the set of possible variations includes those defined on any reference located on the "+
			"path between the target reference and the ancestor-constraining reference, in the alignment tree. "+
			"The <featureName> arguments specifies a feature location on the ancestor-constraining reference. "+
			"If --descendentFeatures is used, variations will also be scanned on the descendent features of the named feature. "+
			"The variation scan will be limited to the specified features. "+
			"The <whereClause> may be used to qualify which variations are scanned for. "+
			"Reads will not contribute to the summary if their reported nucleotide quality score at any point within the variation's region is less than "+
			"<minQScore> (default value is derived from the module config). \n"+
			"No result will be generated for a variation if the number of contributing reads is less than <minDepth> "+
			"(default value is derived from the module config).\n"+
			"Scanned variations will only display in the result if the percentage of reads where the variation is present is at least <minPresentPct> (default 0), and "+
			"if the percentage of reads where it is absent is at least <minAbsentPct> (default 0).",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamVariationScanCommand extends AlignmentTreeSamReporterCommand<SamVariationScanResult> 
	implements ProvidedProjectModeCommand{

	public static final String WHERE_CLAUSE = "whereClause";
	public static final String MULTI_REFERENCE = "multiReference";
	public static final String DESCENDENT_FEATURES = "descendentFeatures";
	
	public static final String MIN_PRESENT_PCT = "minPresentPct";
	public static final String MIN_ABSENT_PCT = "minAbsentPct";
	
	private Expression whereClause;
	private Boolean multiReference;
	private Boolean descendentFeatures;
	private Double minPresentPct;
	private Double minAbsentPct;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.multiReference = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, MULTI_REFERENCE, false)).orElse(false);
		this.descendentFeatures = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DESCENDENT_FEATURES, false)).orElse(false);
	
		this.minPresentPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_PRESENT_PCT, 0.0, true, 100.0, true, false)).orElse(0.0);
		this.minAbsentPct = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MIN_ABSENT_PCT, 0.0, true, 100.0, true, false)).orElse(0.0);
	}


	@Override
	protected SamVariationScanResult execute(CommandContext cmdContext, SamReporter samReporter) {
		Feature namedFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(getFeatureName()));
		
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		SamRefInfo samRefInfo = getSamRefInfo(consoleCmdContext, samReporter);
		
		DNASequence consensusSequence = null;
		ReferenceSequence targetRef;
		AlignmentMember tipAlmtMember;
		if(useMaxLikelihoodPlacer()) {
			Map<String, DNASequence> consensusMap = SamUtils.getSamConsensus(consoleCmdContext, getFileName(), 
					samReporter.getSamReaderValidationStringency(), getSuppliedSamRefName(),"samConsensus", getMinQScore(samReporter), getMinDepth(samReporter), getSamRefSense(samReporter));
			consensusSequence = consensusMap.get("samConsensus");
			tipAlmtMember = samReporter.establishTargetRefMemberUsingPlacer(consoleCmdContext, consensusSequence);
			targetRef = tipAlmtMember.targetReferenceFromMember();
			samReporter.log(Level.FINE, "Max likelihood placement of consensus sequence selected target reference "+targetRef.getName());
		} else {
			targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
					ReferenceSequence.pkMap(establishTargetRefName(consoleCmdContext, samReporter, samRefInfo.getSamRefName(), consensusSequence)));
			tipAlmtMember = targetRef.getTipAlignmentMembership(getTipAlmtName(consoleCmdContext, samReporter, samRefInfo.getSamRefName()));
		}

		Alignment tipAlmt = tipAlmtMember.getAlignment();

		List<ReferenceSequence> refsToScan;
		if(multiReference) {
			refsToScan = tipAlmt.getAncestorPathReferences(cmdContext, getAcRefName());
			if(!refsToScan.contains(targetRef)) {
				refsToScan.add(0, targetRef);
			}
		} else {
			refsToScan = Arrays.asList(tipAlmt.getAncConstrainingRef(cmdContext, getAcRefName()));
		}

		List<Feature> featuresToScan = new ArrayList<Feature>();
		featuresToScan.add(namedFeature);
		if(descendentFeatures) {
			featuresToScan.addAll(namedFeature.getDescendents());
		}
		
		List<VariationScanReadCount> variationScanReadCounts = new ArrayList<VariationScanReadCount>();
		
		for(ReferenceSequence refToScan: refsToScan) {

			for(Feature featureToScan: featuresToScan) {

				samReporter.log(Level.FINE, "Scanning for variations defined on reference: "+refToScan.getName()+", feature: "+featureToScan.getName());

				FeatureLocation featureLoc = 
						GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
								FeatureLocation.pkMap(refToScan.getName(), featureToScan.getName()), true);
				if(featureLoc == null) {
					continue;
				}

				// build a segment tree of the variations.
				List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, whereClause);
				if(variationsToScan.isEmpty()) {
					continue;
				}
				ReferenceSegmentTree<PatternLocation> patternLocSegmentTree = new ReferenceSegmentTree<PatternLocation>();
				for(Variation variation: variationsToScan) {
					variation.getPatternLocs().forEach(ploc -> patternLocSegmentTree.add(ploc));
				}

				Feature feature = featureLoc.getFeature();

				boolean codesAminoAcids = feature.codesAminoAcids();
				Integer codon1Start = codesAminoAcids ? featureLoc.getCodon1Start(cmdContext) : null;
				Translator translator = codesAminoAcids ? new CommandContextTranslator(cmdContext) : null;


				List<QueryAlignedSegment> samRefToTargetRefSegs = getSamRefToTargetRefSegs(cmdContext, samReporter, consoleCmdContext, targetRef, consensusSequence);

				// translate segments to tip alignment reference
				List<QueryAlignedSegment> samRefToTipAlmtRefSegs = tipAlmt.translateToRef(cmdContext, 
						tipAlmtMember.getSequence().getSource().getName(), tipAlmtMember.getSequence().getSequenceID(), 
						samRefToTargetRefSegs);

				// translate segments to scanned reference
				List<QueryAlignedSegment> samRefToScannedRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, samRefToTipAlmtRefSegs, refToScan);

				// trim down to the feature area.
				List<ReferenceSegment> featureRefSegs = featureLoc.getSegments().stream()
						.map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());
				List<QueryAlignedSegment> samRefToScannedRefSegs = 
						ReferenceSegment.intersection(samRefToScannedRefSegsFull, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());

				Map<String, VariationInfo> variationNameToInfo = new LinkedHashMap<String, VariationInfo>();

				SamRefSense samRefSense = getSamRefSense(samReporter);

				try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, getFileName(), 
						samReporter.getSamReaderValidationStringency())) {

					SamRecordFilter samRecordFilter = new SamUtils.ReferenceBasedRecordFilter(samReader, getFileName(), getSuppliedSamRefName());

					final RecordsCounter recordsCounter = samReporter.new RecordsCounter();


					SamUtils.iterateOverSamReader(samReader, samRecord -> {
						if(!samRecordFilter.recordPasses(samRecord)) {
							return;
						}

						List<QueryAlignedSegment> readToSamRefSegs = samReporter.getReadToSamRefSegs(samRecord);
						String readString = samRecord.getReadString().toUpperCase();
						String qualityString = samRecord.getBaseQualityString();
		        		if(samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
		        			readToSamRefSegs = QueryAlignedSegment.reverseSense(readToSamRefSegs, readString.length(), samRefInfo.getSamRefLength());
		        			readString = FastaUtils.reverseComplement(readString);
		        			qualityString = StringUtils.reverseString(qualityString);
		        		}

						List<QueryAlignedSegment> readToScannedRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, samRefToScannedRefSegs);
						List<QueryAlignedSegment> readToScannedRefSegsMerged = 
								ReferenceSegment.mergeAbutting(readToScannedRefSegs, 
										QueryAlignedSegment.mergeAbuttingFunctionQueryAlignedSegment(), 
										QueryAlignedSegment.abutsPredicateQueryAlignedSegment());

						List<VariationScanResult> variationScanResults = new ArrayList<VariationScanResult>();
						for(QueryAlignedSegment readToScannedRefSeg: readToScannedRefSegsMerged) {

							List<PatternLocation> patternLocsToScanForSegment = new LinkedList<PatternLocation>();
							patternLocSegmentTree.findOverlapping(readToScannedRefSeg.getRefStart(), readToScannedRefSeg.getRefEnd(), patternLocsToScanForSegment);
							// remove those pattern locs where the read quality is not good enough.
							patternLocsToScanForSegment = filterPatternLocsOnReadQuality(getMinQScore(samReporter), qualityString, readToScannedRefSeg, patternLocsToScanForSegment);
							
							if(!patternLocsToScanForSegment.isEmpty()) {
								NtQueryAlignedSegment readToScannedRefNtSeg = 
										new NtQueryAlignedSegment(
												readToScannedRefSeg.getRefStart(), readToScannedRefSeg.getRefEnd(), readToScannedRefSeg.getQueryStart(), readToScannedRefSeg.getQueryEnd(),
												SegmentUtils.base1SubString(readString, readToScannedRefSeg.getQueryStart(), readToScannedRefSeg.getQueryEnd()));
								
								List<Variation> variationsToScanForSegment = findVariationsFromPatternLocs(patternLocsToScanForSegment);
								
								variationScanResults.addAll(featureLoc.variationScanSegment(cmdContext, translator, codon1Start, readToScannedRefNtSeg, variationsToScanForSegment, false));
							}
						}

						for(VariationScanResult variationScanResult: variationScanResults) {
							String variationName = variationScanResult.getVariationName();
							VariationInfo variationInfo = variationNameToInfo.get(variationName);
							if(variationInfo == null) {
								variationInfo = new VariationInfo(variationScanResult.getVariationPkMap(), variationScanResult.getMinLocStart(), variationScanResult.getMaxLocEnd());
								variationNameToInfo.put(variationName, variationInfo);
							}
							variationInfo.contributingReads++;
							if(variationScanResult.isPresent()) {
								variationInfo.readsConfirmedPresent++;
							} else {
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

				final int minDepth = getMinDepth(samReporter);
				
				variationScanReadCounts.addAll(
						variationInfos.stream()
						.filter(vInfo -> vInfo.contributingReads >= minDepth)
						.map(vInfo -> {
							int readsWherePresent = vInfo.readsConfirmedPresent;
							int readsWhereAbsent = vInfo.readsConfirmedAbsent;
							double pctWherePresent = 100.0 * readsWherePresent / (readsWherePresent + readsWhereAbsent);
							double pctWhereAbsent = 100.0 * readsWhereAbsent / (readsWherePresent + readsWhereAbsent);
							return new VariationScanReadCount(vInfo.variationPkMap,
									vInfo.minLocStart, vInfo.maxLocEnd,
									readsWherePresent, pctWherePresent, 
									readsWhereAbsent, pctWhereAbsent);
						})
						.collect(Collectors.toList())
						);
			}
		}
		variationScanReadCounts = variationScanReadCounts
				.stream()
				.filter(vsrc -> vsrc.getPctWherePresent() >= minPresentPct)
				.filter(vsrc -> vsrc.getPctWhereAbsent() >= minAbsentPct)
				.collect(Collectors.toList());
		VariationScanReadCount.sortVariationScanReadCounts(variationScanReadCounts);
		return new SamVariationScanResult(variationScanReadCounts);
		
	}


	private List<PatternLocation> filterPatternLocsOnReadQuality(int minQScore,
			String qualityString, QueryAlignedSegment readToScannedRefSeg,
			List<PatternLocation> patternLocs) {
		List<PatternLocation> filteredPatternLocs = new ArrayList<PatternLocation>();
		for(PatternLocation patternLoc: patternLocs) {
			List<QueryAlignedSegment> intersection = 
					ReferenceSegment.intersection(Arrays.asList(patternLoc.asReferenceSegment()), Arrays.asList(readToScannedRefSeg), QueryAlignedSegment.cloneRightSegMerger());
			if(intersection.size() == 1) {
				QueryAlignedSegment readSection = intersection.get(0);
				String qualitySubString = 
						SegmentUtils.base1SubString(qualityString, readSection.getQueryStart(), readSection.getQueryEnd());
				boolean qualityPass = true;
				for(int i = 0; i < qualitySubString.length(); i++) {
					char qualityChar = qualitySubString.charAt(i);
					if(SamUtils.qualityCharToQScore(qualityChar) < minQScore) {
						qualityPass = false;
						break;
					}
				}
				if(qualityPass) {
					filteredPatternLocs.add(patternLoc);
				}
			}
		}
		return filteredPatternLocs;
	}
	
	private List<Variation> findVariationsFromPatternLocs(List<PatternLocation> patternLocsToScanForSegment) {
		Map<Variation, List<PatternLocation>> variationToLocs = new LinkedHashMap<Variation, List<PatternLocation>>();
		patternLocsToScanForSegment.forEach(loc -> {
			Variation variation = loc.getVariation();
			variationToLocs.computeIfAbsent(variation, v -> new ArrayList<PatternLocation>()).add(loc);
		});
		List<Variation> variations = new ArrayList<Variation>();
		variationToLocs.forEach((v, pLocs) -> {
			if(v.getPatternLocs().size() == pLocs.size()) {
				variations.add(v);
			}
		});
		return variations;
	}

	private class VariationInfo {
		Map<String, String> variationPkMap;
		int minLocStart, maxLocEnd;
		int contributingReads = 0;
		int readsConfirmedPresent = 0;
		int readsConfirmedAbsent = 0;
		public VariationInfo(Map<String,String> variationPkMap, int minLocStart, int maxLocEnd) {
			super();
			this.variationPkMap = variationPkMap;
			this.minLocStart = minLocStart;
			this.maxLocEnd = maxLocEnd;
		}
	}
	
	@CompleterClass
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {
		public Completer() {
			super();
			registerEnumLookup("samRefSense", SamRefSense.class);
		}
	}


	
}
