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
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationScanResult;
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

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a SAM/BAM file for variations", 
		docoptUsages = { "-i <fileName> [-s <samRefName>] -r <acRefName> [-m] -f <featureName> [-l] [-t <targetRefName>] [-a <tipAlmtName>] [-w <whereClause>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 SAM/BAM input file",
				"-s <samRefName>, --samRefName <samRefName>           Specific SAM ref seq",
				"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
				"-m, --multiReference                                 Scan across references",
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
			"If <targetRefName> is not supplied, it may be inferred from the SAM reference name, if the module is appropriately configured. "+
			"By default, the SAM file is assumed to align reads against this target reference, i.e. the target GLUE reference "+
			"is the reference sequence  mentioned in the SAM file. "+
			"Alternatively the --autoAlign option may be used; this will generate a pairwise alignment between the SAM file "+
			"consensus and the target GLUE reference. \n"+
			"The target reference sequence must be a member of a constrained "+
			"'tip alignment'. The tip alignment may be specified by <tipAlmtName>. If unspecified, it will be "+
			"inferred from the target reference if possible. "+
			"The <acRefName> argument specifies an 'ancestor-constraining' reference sequence. "+
			"This must be the constraining reference of an ancestor alignment of the tip alignment. "+
			"If --multiReference is used, the set of possible variations includes those defined on any reference located on the "+
			"path between the target reference and the ancestor-constraining reference, in the alignment tree. "+
			"The <featureName> arguments specifies a feature location on the ancestor-constraining reference. "+
			"The variation scan will be limited to the specified feature location. "+
			"The <whereClause> may be used to qualify which variations are scanned for. ",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamVariationScanCommand extends SamReporterCommand<SamVariationScanResult> 
	implements ProvidedProjectModeCommand{

	public static final String WHERE_CLAUSE = "whereClause";
	public static final String MULTI_REFERENCE = "multiReference";
	
	private Expression whereClause;
	private Boolean multiReference;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.multiReference = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, MULTI_REFERENCE, false)).orElse(false);

	}


	@Override
	protected SamVariationScanResult execute(CommandContext cmdContext, SamReporter samReporter) {
		// check feature exists.
		GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(getFeatureName()));
		
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		String samRefName;
		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, getFileName())) {
			samRefName = SamUtils.findReference(samReader, getFileName(), getSuppliedSamRefName()).getSequenceName();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, 
				ReferenceSequence.pkMap(getTargetRefName(consoleCmdContext, samReporter, samRefName)));
		
		AlignmentMember tipAlmtMember = targetRef.getTipAlignmentMembership(getTipAlmtName());
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

		List<VariationScanReadCount> variationScanReadCounts = new ArrayList<VariationScanReadCount>();
		
		for(ReferenceSequence refToScan: refsToScan) {
			samReporter.log(Level.FINE, "Scanning for variations defined on reference: "+refToScan.getName());
			
			FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(refToScan.getName(), getFeatureName()), true);
			if(featureLoc == null) {
				continue;
			}
			
			// build a segment tree of the variations.
			List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, whereClause);
			if(variationsToScan.isEmpty()) {
				continue;
			}
			ReferenceSegmentTree<Variation> variationSegmentTree = new ReferenceSegmentTree<Variation>();
			for(Variation variation: variationsToScan) {
				variationSegmentTree.add(variation);
			}

			Feature feature = featureLoc.getFeature();

			boolean codesAminoAcids = feature.codesAminoAcids();
			Integer codon1Start = codesAminoAcids ? featureLoc.getCodon1Start(cmdContext) : null;
			Translator translator = codesAminoAcids ? new CommandContextTranslator(cmdContext) : null;


			List<QueryAlignedSegment> samRefToTargetRefSegs = getSamRefToTargetRefSegs(cmdContext, samReporter, consoleCmdContext, targetRef);

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


			try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, getFileName())) {

				SamRecordFilter samRecordFilter = new SamUtils.ReferenceBasedRecordFilter(samReader, getFileName(), getSuppliedSamRefName());

				final RecordsCounter recordsCounter = samReporter.new RecordsCounter();


				samReader.forEach(samRecord -> {
					if(!samRecordFilter.recordPasses(samRecord)) {
						return;
					}
					List<QueryAlignedSegment> readToSamRefSegs = samReporter.getReadToSamRefSegs(samRecord);
					List<QueryAlignedSegment> readToAncConstrRefSegs = QueryAlignedSegment.translateSegments(readToSamRefSegs, samRefToAncConstrRefSegs);

					final String readString = samRecord.getReadString().toUpperCase();

					List<QueryAlignedSegment> readToAncConstrRefSegsMerged = 
							ReferenceSegment.mergeAbutting(readToAncConstrRefSegs, QueryAlignedSegment.mergeAbuttingFunction());

					List<VariationScanResult> variationScanResults = new ArrayList<VariationScanResult>();
					for(QueryAlignedSegment readToAncConstrRefSeg: readToAncConstrRefSegsMerged) {

						List<Variation> variationsToScanForSegment = new LinkedList<Variation>();
						variationSegmentTree.findOverlapping(readToAncConstrRefSeg.getRefStart(), readToAncConstrRefSeg.getRefEnd(), variationsToScanForSegment);
						if(!variationsToScanForSegment.isEmpty()) {
							NtQueryAlignedSegment readToAncConstrRefNtSeg = 
									new NtQueryAlignedSegment(
											readToAncConstrRefSeg.getRefStart(), readToAncConstrRefSeg.getRefEnd(), readToAncConstrRefSeg.getQueryStart(), readToAncConstrRefSeg.getQueryEnd(),
											SegmentUtils.base1SubString(readString, readToAncConstrRefSeg.getQueryStart(), readToAncConstrRefSeg.getQueryEnd()));

							variationScanResults.addAll(featureLoc.variationScanSegment(translator, codon1Start, readToAncConstrRefNtSeg, variationsToScanForSegment));
						}
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

			variationScanReadCounts.addAll(
					variationInfos.stream()
					.map(vInfo -> {
						int readsWherePresent = vInfo.readsConfirmedPresent;
						int readsWhereAbsent = vInfo.readsConfirmedAbsent;
						double pctWherePresent = 100.0 * readsWherePresent / (readsWherePresent + readsWhereAbsent);
						double pctWhereAbsent = 100.0 * readsWhereAbsent / (readsWherePresent + readsWhereAbsent);
						return new VariationScanReadCount(vInfo.variation, 
								readsWherePresent, pctWherePresent, 
								readsWhereAbsent, pctWhereAbsent);
					})
					.collect(Collectors.toList())
					);
		}
		VariationScanReadCount.sortVariationScanReadCounts(variationScanReadCounts);
		return new SamVariationScanResult(variationScanReadCounts);
		
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
