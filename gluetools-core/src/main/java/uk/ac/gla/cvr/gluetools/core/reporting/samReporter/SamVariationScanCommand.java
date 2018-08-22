/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
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
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor.SamFileSession;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamVariationScanCommand.VariationContext;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamVariationScanCommand.VariationResult;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegmentTree;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.variationscanner.BaseVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a SAM/BAM file for variations", 
		docoptUsages = { "-i <fileName> [-n <samRefSense>] [-s <samRefName>] -r <relRefName> [-m] -f <featureName> [-d] (-p | [-l] -t <targetRefName>) -a <linkingAlmtName> [-w <whereClause>] [-q <minQScore>] [-g <minMapQ>] [-e <minDepth>] [-P <minPresentPct>] [-A <minAbsentPct>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                       SAM/BAM input file",
				"-n <samRefSense>, --samRefSense <samRefSense>              SAM ref seq sense",
				"-s <samRefName>, --samRefName <samRefName>                 Specific SAM ref seq",
				"-r <relRefName>, --relRefName <relRefName>                 Related reference sequence",
				"-m, --multiReference                                       Scan across references",
				"-f <featureName>, --featureName <featureName>              Feature to translate",
				"-d, --descendentFeatures                                   Include descendent features",
				"-p, --maxLikelihoodPlacer                                  Use ML placer module",
				"-l, --autoAlign                                            Auto-align consensus",
				"-t <targetRefName>, --targetRefName <targetRefName>        Target GLUE reference",
				"-a <linkingAlmtName>, --linkingAlmtName <linkingAlmtName>  Linking alignment",
				"-w <whereClause>, --whereClause <whereClause>              Qualify variations",
				"-q <minQScore>, --minQScore <minQScore>                    Minimum Phred quality score",
				"-g <minMapQ>, --minMapQ <minMapQ>                          Minimum mapping quality score",
				"-e <minDepth>, --minDepth <minDepth>                       Minimum depth",
				"-P <minPresentPct>, --minPresentPct <minPresentPct>        Show present at minimum percentage",
				"-A <minAbsentPct>, --minAbsentPct <minAbsentPct>           Show absent at minimum percentage",
		},
		furtherHelp = 
			"This command scans a SAM/BAM file for variations. "+
			"If <samRefName> is supplied, the scan limited to those reads which are aligned to the "+
			"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
			"file only names a single reference sequence.\n"+
			"The translation is based on a 'target' GLUE reference sequence. "+
			"The <samRefSense> may be FORWARD or REVERSE_COMPLEMENT, indicating the presumed sense of the SAM reference, relative to the GLUE references."+
			"If the --maxLikelihoodPlacer option is used, an ML placement is performed, and the target reference is "+
			"identified as the closest according to this placement. "+
			"Otherwise, the target reference must be specified using <targetRefName>."+
			"By default, the SAM file is assumed to align reads against this target reference, i.e. the target GLUE reference "+
			"is the reference sequence mentioned in the SAM file. "+
			"Alternatively the --autoAlign option may be used; this will generate a pairwise alignment between the SAM file "+
			"consensus and the target GLUE reference. \n"+
			"The --autoAlign option is implicit if --maxLikelihoodPlacer is used. "+
			"The target reference sequence must be a member of the "+
			"'linking alignment', specified by <linkingAlmtName>. "+
	        "The <relRefName> argument specifies the related reference sequence, on which the feature is defined. "+
			"If the linking alignment is constrained, the related reference must constrain an ancestor alignment "+
	        "of the linking alignment. Otherwise, it may be any reference sequence which shares membership of the "+
			"linking alignment with the target reference. "+
			"If the linking alignment is constrained, --multiReference may be used. If so, "+
			"the set of possible variations includes those defined on any reference located on the "+
			"path between the target reference and the ancestor-constraining reference, in the alignment tree. "+
			"The <featureName> arguments specifies a feature location on the related reference. "+
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
public class SamVariationScanCommand extends ReferenceLinkedSamReporterCommand<SamVariationScanResult> 
	implements ProvidedProjectModeCommand, SamPairedParallelProcessor<VariationContext, VariationResult>{

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
		String samFileName = getFileName();
		ValidationStringency validationStringency = samReporter.getSamReaderValidationStringency();
		
		try(SamFileSession samFileSession = SamReporterPreprocessor.preprocessSam(consoleCmdContext, samFileName, validationStringency)) {
			if(useMaxLikelihoodPlacer()) {
				Map<String, DNASequence> consensusMap = SamUtils.getSamConsensus(consoleCmdContext, samFileName, samFileSession,
						validationStringency, getSuppliedSamRefName(),"samConsensus", getMinQScore(samReporter), getMinMapQ(samReporter), getMinDepth(samReporter), getSamRefSense(samReporter));
				consensusSequence = consensusMap.get("samConsensus");
				AlignmentMember targetRefAlmtMember = samReporter.establishTargetRefMemberUsingPlacer(consoleCmdContext, consensusSequence);
				targetRef = targetRefAlmtMember.targetReferenceFromMember();
				samReporter.log(Level.FINE, "Max likelihood placement of consensus sequence selected target reference "+targetRef.getName());
			} else {
				targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
						ReferenceSequence.pkMap(getTargetRefName()));
			}

			Alignment linkingAlmt = GlueDataObject.lookup(cmdContext, Alignment.class, 
					Alignment.pkMap(getLinkingAlmtName()));
			ReferenceSequence relatedRef = linkingAlmt.getRelatedRef(cmdContext, getRelatedRefName());

			List<ReferenceSequence> refsToScan;
			if(multiReference) {
				refsToScan = linkingAlmt.getAncestorPathReferences(cmdContext, getRelatedRefName());
				if(!refsToScan.contains(targetRef)) {
					refsToScan.add(0, targetRef);
				}
			} else {
				refsToScan = Arrays.asList(relatedRef);
			}

			List<Feature> featuresToScan = new ArrayList<Feature>();
			featuresToScan.add(namedFeature);
			if(descendentFeatures) {
				featuresToScan.addAll(namedFeature.getDescendents());
			}
			
			List<QueryAlignedSegment> samRefToTargetRefSegs = getSamRefToTargetRefSegs(cmdContext, samReporter, samFileSession, consoleCmdContext, targetRef, consensusSequence);

			AlignmentMember linkingAlmtMember = targetRef.getLinkingAlignmentMembership(getLinkingAlmtName());

			// translate segments to linking alignment coords
			List<QueryAlignedSegment> samRefToLinkingAlmtSegs = linkingAlmt.translateToAlmt(cmdContext, 
					linkingAlmtMember.getSequence().getSource().getName(), linkingAlmtMember.getSequence().getSequenceID(), 
					samRefToTargetRefSegs);
			
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

					List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, whereClause);
					if(variationsToScan.isEmpty()) {
						continue;
					}
					// translate segments to scanned reference
					List<QueryAlignedSegment> samRefToScannedRefSegsFull = linkingAlmt.translateToRelatedRef(cmdContext, samRefToLinkingAlmtSegs, refToScan);

					// trim down to the feature area.
					List<ReferenceSegment> featureRefSegs = featureLoc.getSegments().stream()
							.map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());
					List<QueryAlignedSegment> samRefToScannedRefSegs = 
							ReferenceSegment.intersection(samRefToScannedRefSegsFull, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());

					SamRefSense samRefSense = getSamRefSense(samReporter);

					Supplier<VariationContext> contextSupplier = () -> {
						VariationContext context = new VariationContext();
						context.samRefInfo = samRefInfo;
						context.samRefSense = samRefSense;
						context.varCovSegTree = new ReferenceSegmentTree<VariationCoverageSegment>();
						synchronized(variationsToScan) {
							// build a segment tree of the variations.
							for(Variation variation: variationsToScan) {
								BaseVariationScanner<?> scanner = variation.getScanner(cmdContext);
								scanner.getSegmentsToCover()
								.forEach(seg2cover -> 
									context.varCovSegTree.add(
										new VariationCoverageSegment(scanner, seg2cover.getRefStart(), seg2cover.getRefEnd())));
							}
						}
						synchronized(samRefToScannedRefSegs) {
							context.samRefToScannedRefSegs = QueryAlignedSegment.cloneList(samRefToScannedRefSegs);
						}
						context.suppliedSamRefName = getSuppliedSamRefName();
						context.samFileName = samFileName;
						context.samReporter = samReporter;
						return context;
					};
					
					VariationResult reducedResult = 
							SamUtils.pairedParallelSamIterate(contextSupplier, consoleCmdContext, samFileSession, validationStringency, this);
					
					List<VariationInfo> variationInfos = new ArrayList<VariationInfo>(reducedResult.variationNameToInfo.values());

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
										vInfo.refStart, vInfo.refEnd,
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
		
	}


	private void recordScanResults(VariationContext context,
			List<VariationScanResult<?>> variationScanResults) {
		// record the presence / absence
		for(VariationScanResult<?> variationScanResult: variationScanResults) {
			String variationName = variationScanResult.getVariationName();
			VariationInfo variationInfo = context.variationNameToInfo.get(variationName);
			if(variationInfo == null) {
				variationInfo = new VariationInfo(variationScanResult.getVariationPkMap(), variationScanResult.getRefStart(), variationScanResult.getRefEnd());
				context.variationNameToInfo.put(variationName, variationInfo);
			}
			if(variationScanResult.isSufficientCoverage()) {
				variationInfo.contributingReads++;
				if(variationScanResult.isPresent()) {
					variationInfo.readsConfirmedPresent++;
				} else {
					variationInfo.readsConfirmedAbsent++;
				} 
			}
		}
	}


	public List<VariationScanResult<?>> scanResultsForRead(
			VariationContext context, SAMRecord samRecord) {
		List<QueryAlignedSegment> readToSamRefSegs = context.samReporter.getReadToSamRefSegs(samRecord);
		String readString = samRecord.getReadString().toUpperCase();
		String qualityString = samRecord.getBaseQualityString();
		if(context.samRefSense.equals(SamRefSense.REVERSE_COMPLEMENT)) {
			readToSamRefSegs = QueryAlignedSegment.reverseSense(readToSamRefSegs, readString.length(), context.samRefInfo.getSamRefLength());
			readString = FastaUtils.reverseComplement(readString);
			qualityString = StringUtils.reverseString(qualityString);
		}

		List<QueryAlignedSegment> readToScannedRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, context.samRefToScannedRefSegs);
		List<QueryAlignedSegment> readToScannedRefSegsMerged = 
				ReferenceSegment.mergeAbutting(readToScannedRefSegs, 
						QueryAlignedSegment.mergeAbuttingFunctionQueryAlignedSegment(), 
						QueryAlignedSegment.abutsPredicateQueryAlignedSegment());

		// find the variations for which any of their coverage segments overlap the read segments.
		List<VariationCoverageSegment> varCovSegs = new ArrayList<VariationCoverageSegment>();
		for(QueryAlignedSegment readToScannedRefSeg: readToScannedRefSegsMerged) {
			List<VariationCoverageSegment> overlappingVarCovSegs = new LinkedList<VariationCoverageSegment>();
			context.varCovSegTree.findOverlapping(readToScannedRefSeg.getRefStart(), readToScannedRefSeg.getRefEnd(), overlappingVarCovSegs);
			// remove those variations where the read quality is not good enough.
			List<VariationCoverageSegment> filteredVarCovSegs = filterVarCovSegsOnReadQuality(getMinQScore(context.samReporter), qualityString, readToScannedRefSeg, overlappingVarCovSegs);
			varCovSegs.addAll(filteredVarCovSegs);
		}

		
		List<VariationScanResult<?>> variationScanResults = new ArrayList<VariationScanResult<?>>();
		if(!varCovSegs.isEmpty()) {
			// convert to ntQaSegments
			List<NtQueryAlignedSegment> readToScannedRefNtSegs = new ArrayList<NtQueryAlignedSegment>();
			for(QueryAlignedSegment readToScannedRefSeg: readToScannedRefSegsMerged) {
				readToScannedRefNtSegs.add(
						new NtQueryAlignedSegment(
								readToScannedRefSeg.getRefStart(), readToScannedRefSeg.getRefEnd(), readToScannedRefSeg.getQueryStart(), readToScannedRefSeg.getQueryEnd(),
								SegmentUtils.base1SubString(readString, readToScannedRefSeg.getQueryStart(), readToScannedRefSeg.getQueryEnd()))
						);
			}
			// find the scanners.
			List<BaseVariationScanner<?>> scannersForSegment = findScannersFromVarCovSegs(varCovSegs);
			// do the scan
			variationScanResults.addAll(FeatureLocation.variationScan(readToScannedRefNtSegs, readString, qualityString, scannersForSegment, false, false));
		}
		return variationScanResults;
	}

	private List<VariationCoverageSegment> filterVarCovSegsOnReadQuality(int minQScore,
			String qualityString, QueryAlignedSegment readToScannedRefSeg,
			List<VariationCoverageSegment> varCovSegs) {
		List<VariationCoverageSegment> filteredVarCovSegs = new ArrayList<VariationCoverageSegment>();
		for(VariationCoverageSegment varCovSeg: varCovSegs) {
			List<QueryAlignedSegment> intersection = 
					ReferenceSegment.intersection(Arrays.asList(varCovSeg), Arrays.asList(readToScannedRefSeg), QueryAlignedSegment.cloneRightSegMerger());
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
					filteredVarCovSegs.add(varCovSeg);
				}
			}
		}
		return filteredVarCovSegs;
	}
	
	private List<BaseVariationScanner<?>> findScannersFromVarCovSegs(List<VariationCoverageSegment> varCovSegs) {
		return new ArrayList<BaseVariationScanner<?>>(varCovSegs.stream().map(vcs -> vcs.getVariationScanner()).collect(Collectors.toSet()));
	}

	private class VariationInfo {
		Map<String, String> variationPkMap;
		int refStart, refEnd;
		int contributingReads = 0;
		int readsConfirmedPresent = 0;
		int readsConfirmedAbsent = 0;
		public VariationInfo(Map<String,String> variationPkMap, int refStart, int refEnd) {
			super();
			this.variationPkMap = variationPkMap;
			this.refStart = refStart;
			this.refEnd = refEnd;
		}
	}
	
	public static class VariationContext {
		public SamRefInfo samRefInfo;
		public String samFileName;
		public String suppliedSamRefName;
		public List<QueryAlignedSegment> samRefToScannedRefSegs;
		public SamReporter samReporter;
		public SamRefSense samRefSense;
		public ReferenceSegmentTree<VariationCoverageSegment> varCovSegTree;
		Map<String, VariationInfo> variationNameToInfo = new LinkedHashMap<String, VariationInfo>();
		public SamRecordFilter samRecordFilter;

	}

	public static class VariationResult {
		Map<String, VariationInfo> variationNameToInfo;

	}

	
	
	@CompleterClass
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {
		public Completer() {
			super();
			registerEnumLookup("samRefSense", SamRefSense.class);
		}
	}

	private class VariationCoverageSegment extends ReferenceSegment {
		private BaseVariationScanner<?> variationScanner;

		public VariationCoverageSegment(BaseVariationScanner<?> variationScanner, int refStart, int refEnd) {
			super(refStart, refEnd);
			this.variationScanner = variationScanner;
		}

		public BaseVariationScanner<?> getVariationScanner() {
			return variationScanner;
		}
	}

	@Override
	public void initContextForReader(VariationContext context, SamReader samReader) {
		context.samRecordFilter = new SamUtils.ConjunctionBasedRecordFilter(
				new SamUtils.ReferenceBasedRecordFilter(samReader, context.samFileName, context.suppliedSamRefName), 
				new SamUtils.MappingQualityRecordFilter(getMinMapQ(context.samReporter))
		);
	}


	@Override
	public void processPair(VariationContext context, SAMRecord samRecord1, SAMRecord samRecord2) {
		if(!context.samRecordFilter.recordPasses(samRecord1)) {
			processSingleton(context, samRecord2);
		} else if(!context.samRecordFilter.recordPasses(samRecord2)) {
			processSingleton(context, samRecord1);
		} else {
			List<VariationScanResult<?>> scanResults1 = scanResultsForRead(context, samRecord1);
			List<VariationScanResult<?>> scanResults2 = scanResultsForRead(context, samRecord2);
			List<VariationScanResult<?>> mergedScanResults = new ArrayList<VariationScanResult<?>>();

			Map<Map<String, String>, VariationScanResult<?>> variationPkMapToScanResult = new LinkedHashMap<Map<String,String>, VariationScanResult<?>>();
			for(VariationScanResult<?> scanResult1: scanResults1) {
				variationPkMapToScanResult.put(scanResult1.getVariationPkMap(), scanResult1);
			}
			for(VariationScanResult<?> scanResult2: scanResults2) {
				VariationScanResult<?> scanResult1 = variationPkMapToScanResult.remove(scanResult2.getVariationPkMap());
				if(scanResult1 != null) {
					BaseVariationScanner<?> scanner = scanResult1.getScanner();
					mergedScanResults.add(scanner.resolvePairedReadResults(samRecord1, scanResult1, samRecord2, scanResult2));
				} else {
					mergedScanResults.add(scanResult2);
				}
			}
			mergedScanResults.addAll(variationPkMapToScanResult.values());
			recordScanResults(context, mergedScanResults);
		}
	}


	@Override
	public void processSingleton(VariationContext context, SAMRecord samRecord) {
		if(context.samRecordFilter.recordPasses(samRecord)) {
			List<VariationScanResult<?>> variationScanResults = scanResultsForRead(context, samRecord);
			recordScanResults(context, variationScanResults);
		}
		
	}


	@Override
	public VariationResult contextResult(VariationContext context) {
		VariationResult variationResult = new VariationResult();
		variationResult.variationNameToInfo = context.variationNameToInfo;
		return variationResult;
	}


	@Override
	public VariationResult reduceResults(VariationResult result1, VariationResult result2) {
		VariationResult reducedResult = new VariationResult();
		reducedResult.variationNameToInfo = result1.variationNameToInfo;
		result2.variationNameToInfo.forEach((name, info) -> {
			VariationInfo existingVInfo = reducedResult.variationNameToInfo.get(name);
			if(existingVInfo == null) {
				reducedResult.variationNameToInfo.put(name, info);
			} else {
				existingVInfo.contributingReads += info.contributingReads;
				existingVInfo.readsConfirmedAbsent += info.readsConfirmedAbsent;
				existingVInfo.readsConfirmedPresent += info.readsConfirmedPresent;
			}
		});
		return reducedResult;

	}
	
}
