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
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegmentTree;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a SAM/BAM file for variations", 
		docoptUsages = { "-i <fileName> [-s <samRefName>] -r <acRefName> [-m] -f <featureName> [-d] (-p | [-l][-t <targetRefName>] [-a <tipAlmtName>] ) [-w <whereClause>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 SAM/BAM input file",
				"-s <samRefName>, --samRefName <samRefName>           Specific SAM ref seq",
				"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
				"-m, --multiReference                                 Scan across references",
				"-f <featureName>, --featureName <featureName>        Feature to translate",
				"-d, --descendentFeatures                             Include descendent features",
				"-p, --maxLikelihoodPlacer                            Use ML placer module",
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
			"The <whereClause> may be used to qualify which variations are scanned for. ",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamVariationScanCommand extends AlignmentTreeSamReporterCommand<SamVariationScanResult> 
	implements ProvidedProjectModeCommand{

	public static final String WHERE_CLAUSE = "whereClause";
	public static final String MULTI_REFERENCE = "multiReference";
	public static final String DESCENDENT_FEATURES = "descendentFeatures";

	
	private Expression whereClause;
	private Boolean multiReference;
	private Boolean descendentFeatures;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.multiReference = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, MULTI_REFERENCE, false)).orElse(false);
		this.descendentFeatures = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DESCENDENT_FEATURES, false)).orElse(false);
	}


	@Override
	protected SamVariationScanResult execute(CommandContext cmdContext, SamReporter samReporter) {
		Feature namedFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(getFeatureName()));
		
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		String samRefName;
		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, getFileName(), 
				samReporter.getSamReaderValidationStringency())) {
			samRefName = SamUtils.findReference(samReader, getFileName(), getSuppliedSamRefName()).getSequenceName();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		DNASequence consensusSequence = null;
		ReferenceSequence targetRef;
		AlignmentMember tipAlmtMember;
		if(useMaxLikelihoodPlacer()) {
			consensusSequence = SamUtils.getSamConsensus(consoleCmdContext, getFileName(), 
					samReporter.getSamReaderValidationStringency(), getSuppliedSamRefName(),"samConsensus").get("samConsensus");
			tipAlmtMember = samReporter.establishTargetRefMemberUsingPlacer(consoleCmdContext, consensusSequence);
			targetRef = tipAlmtMember.targetReferenceFromMember();
		} else {
			targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
					ReferenceSequence.pkMap(establishTargetRefName(consoleCmdContext, samReporter, samRefName, consensusSequence)));
			tipAlmtMember = targetRef.getTipAlignmentMembership(getTipAlmtName(consoleCmdContext, samReporter, samRefName));
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
				ReferenceSegmentTree<PatternLocation> variationSegmentTree = new ReferenceSegmentTree<PatternLocation>();
				for(Variation variation: variationsToScan) {
					variation.getPatternLocs().forEach(ploc -> variationSegmentTree.add(ploc));
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

				// translate segments to ancestor constraining reference
				List<QueryAlignedSegment> samRefToAncConstrRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, samRefToTipAlmtRefSegs, refToScan);

				// trim down to the feature area.
				List<ReferenceSegment> featureRefSegs = featureLoc.getSegments().stream()
						.map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());
				List<QueryAlignedSegment> samRefToAncConstrRefSegs = 
						ReferenceSegment.intersection(samRefToAncConstrRefSegsFull, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());

				Map<String, VariationInfo> variationNameToInfo = new LinkedHashMap<String, VariationInfo>();


				try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, getFileName(), 
						samReporter.getSamReaderValidationStringency())) {

					SamRecordFilter samRecordFilter = new SamUtils.ReferenceBasedRecordFilter(samReader, getFileName(), getSuppliedSamRefName());

					final RecordsCounter recordsCounter = samReporter.new RecordsCounter();


					SamUtils.iterateOverSamReader(samReader, samRecord -> {
						if(!samRecordFilter.recordPasses(samRecord)) {
							return;
						}
						List<QueryAlignedSegment> readToSamRefSegs = samReporter.getReadToSamRefSegs(samRecord);
						List<QueryAlignedSegment> readToAncConstrRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, samRefToAncConstrRefSegs);

						final String readString = samRecord.getReadString().toUpperCase();

						List<QueryAlignedSegment> readToAncConstrRefSegsMerged = 
								ReferenceSegment.mergeAbutting(readToAncConstrRefSegs, 
										QueryAlignedSegment.mergeAbuttingFunctionQueryAlignedSegment(), 
										QueryAlignedSegment.abutsPredicateQueryAlignedSegment());

						List<VariationScanResult> variationScanResults = new ArrayList<VariationScanResult>();
						for(QueryAlignedSegment readToAncConstrRefSeg: readToAncConstrRefSegsMerged) {

							List<PatternLocation> patternLocsToScanForSegment = new LinkedList<PatternLocation>();
							variationSegmentTree.findOverlapping(readToAncConstrRefSeg.getRefStart(), readToAncConstrRefSeg.getRefEnd(), patternLocsToScanForSegment);
							if(!patternLocsToScanForSegment.isEmpty()) {
								NtQueryAlignedSegment readToAncConstrRefNtSeg = 
										new NtQueryAlignedSegment(
												readToAncConstrRefSeg.getRefStart(), readToAncConstrRefSeg.getRefEnd(), readToAncConstrRefSeg.getQueryStart(), readToAncConstrRefSeg.getQueryEnd(),
												SegmentUtils.base1SubString(readString, readToAncConstrRefSeg.getQueryStart(), readToAncConstrRefSeg.getQueryEnd()));
								
								List<Variation> variationsToScanForSegment = findVariationsFromPatternLocs(patternLocsToScanForSegment);
								
								variationScanResults.addAll(featureLoc.variationScanSegment(cmdContext, translator, codon1Start, readToAncConstrRefNtSeg, variationsToScanForSegment, false));
							}
						}

						for(VariationScanResult variationScanResult: variationScanResults) {
							String variationName = variationScanResult.getVariationName();
							VariationInfo variationInfo = variationNameToInfo.get(variationName);
							if(variationInfo == null) {
								variationInfo = new VariationInfo(variationScanResult.getVariationPkMap(), variationScanResult.getMinLocStart(), variationScanResult.getMaxLocEnd());
								variationNameToInfo.put(variationName, variationInfo);
							}
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

				variationScanReadCounts.addAll(
						variationInfos.stream()
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
		VariationScanReadCount.sortVariationScanReadCounts(variationScanReadCounts);
		return new SamVariationScanResult(variationScanReadCounts);
		
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
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {}


	
}
