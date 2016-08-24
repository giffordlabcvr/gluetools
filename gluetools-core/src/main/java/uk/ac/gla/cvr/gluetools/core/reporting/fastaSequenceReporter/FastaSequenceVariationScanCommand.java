package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
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
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a FASTA file for variations", 
		docoptUsages = { "-i <fileName> -r <acRefName> [-m] -f <featureName> [-d] [-t <targetRefName>] [-a <tipAlmtName>] [-w <whereClause>] [-e]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 FASTA input file",
				"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
				"-m, --multiReference                                 Scan across references",
				"-f <featureName>, --featureName <featureName>        Feature to scan",
				"-d, --descendentFeatures                             Include descendent features",
				"-t <targetRefName>, --targetRefName <targetRefName>  Target reference",
				"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
				"-w <whereClause>, --whereClause <whereClause>        Qualify variations",
				"-e, --excludeAbsent                                  Exclude absent variations",
		},
		furtherHelp = 
		        "This command aligns a FASTA query sequence to a 'target' reference sequence, and "+
		        "scans a section of the query "+
				"If <targetRefName> is not supplied, it may be inferred from the FASTA sequence ID, if the module is appropriately configured. "+
				"sequence for variations based on the target reference sequence's "+
				"place in the alignment tree. The target reference sequence must be a member of a constrained "+
		        "'tip alignment'. The tip alignment may be specified by <tipAlmtName>. If unspecified, it will be "+
		        "inferred from the target reference if possible. "+
		        "The <acRefName> argument specifies an 'ancestor-constraining' reference sequence. "+
				"This must be the constraining reference of an ancestor alignment of the tip alignment. "+
				"If --multiReference is used, the set of possible variations includes those defined on any reference located on the "+
				"path between the target reference and the ancestor-constraining reference, in the alignment tree. "+
				"The <featureName> arguments specifies a feature location on the ancestor-constraining reference. "+
				"If --descendentFeatures is used, variations will also be scanned on the descendent features of the named feature. "+
				"The variation scan will be limited to the specified features. "+
				"If <whereClause> is used, this qualifies the set of variations which are scanned for "+
				"If --excludeAbsent is used, variations which were confirmed to be absent will not appear in the results.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class FastaSequenceVariationScanCommand extends FastaSequenceReporterCommand<FastaSequenceVariationScanResult> 
	implements ProvidedProjectModeCommand{

	public static final String WHERE_CLAUSE = "whereClause";
	public static final String MULTI_REFERENCE = "multiReference";
	public static final String DESCENDENT_FEATURES = "descendentFeatures";
	public static final String EXCLUDE_ABSENT = "excludeAbsent";

	private Expression whereClause;
	private Boolean multiReference;
	private Boolean descendentFeatures;
	private Boolean excludeAbsent;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.multiReference = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, MULTI_REFERENCE, false)).orElse(false);
		this.descendentFeatures = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DESCENDENT_FEATURES, false)).orElse(false);
		this.excludeAbsent = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_ABSENT, false)).orElse(false);
	}

	@Override
	protected FastaSequenceVariationScanResult execute(CommandContext cmdContext,
			FastaSequenceReporter fastaSequenceReporter) {

		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		
		Entry<String, DNASequence> fastaEntry = getFastaEntry(consoleCmdContext);
		String fastaID = fastaEntry.getKey();
		DNASequence fastaNTSeq = fastaEntry.getValue();

		String targetRefName = Optional.ofNullable(getTargetRefName())
				.orElse(fastaSequenceReporter.targetRefNameFromFastaId(consoleCmdContext, fastaID));
		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));

		AlignmentMember tipAlmtMember = targetRef.getTipAlignmentMembership(getTipAlmtName());
		Alignment tipAlmt = tipAlmtMember.getAlignment();

		ReferenceSequence ancConstrRef = tipAlmt.getAncConstrainingRef(cmdContext, getAcRefName());

		// align query to target reference
		Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, fastaSequenceReporter.getAlignerModuleName());
		AlignerResult alignerResult = aligner.computeConstrained(cmdContext, targetRef.getName(), fastaID, fastaNTSeq);

		// extract segments from aligner result
		List<QueryAlignedSegment> queryToTargetRefSegs = alignerResult.getQueryIdToAlignedSegments().get(fastaID);

		// translate segments to tip alignment reference
		List<QueryAlignedSegment> queryToTipAlmtRefSegs = tipAlmt.translateToRef(cmdContext, 
				tipAlmtMember.getSequence().getSource().getName(), tipAlmtMember.getSequence().getSequenceID(), 
				queryToTargetRefSegs);


		List<VariationScanResult> variationScanResults = variationScan(
				cmdContext, getFeatureName(), fastaNTSeq, targetRef.getName(), tipAlmt,
				ancConstrRef.getName(), queryToTipAlmtRefSegs, 
				multiReference, descendentFeatures, excludeAbsent, whereClause);
		
		return new FastaSequenceVariationScanResult(variationScanResults);
	}

	public static List<VariationScanResult> variationScan(CommandContext cmdContext,
			String featureName, DNASequence fastaNTSeq,
			String targetRefName, Alignment tipAlmt,
			String ancConstrRefName,
			List<QueryAlignedSegment> queryToTipAlmtRefSegs, 
			boolean multiReference, boolean descendentFeatures, boolean excludeAbsent,
			Expression variationWhereClause) {
		Feature namedFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));

		List<ReferenceSequence> refsToScan;
		ReferenceSequence targetRef = 
				GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));
		ReferenceSequence ancConstrRef = 
				GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(ancConstrRefName));

		if(multiReference) {
			refsToScan = tipAlmt.getAncestorPathReferences(cmdContext, ancConstrRefName);
			if(!refsToScan.contains(targetRef)) {
				refsToScan.add(0, targetRef);
			}
		} else {
			refsToScan = Arrays.asList(ancConstrRef);
		}

		List<Feature> featuresToScan = new ArrayList<Feature>();
		featuresToScan.add(namedFeature);
		if(descendentFeatures) {
			featuresToScan.addAll(namedFeature.getDescendents());
		}

		
		List<VariationScanResult> variationScanResults = new ArrayList<VariationScanResult>();
		
		for(ReferenceSequence refToScan: refsToScan) {

			for(Feature featureToScan: featuresToScan) {

				FeatureLocation featureLoc = 
						GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
								FeatureLocation.pkMap(refToScan.getName(), featureToScan.getName()), true);
				if(featureLoc == null) {
					continue;
				}
				
				List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, variationWhereClause);
				if(variationsToScan.isEmpty()) {
					continue;
				}
	
				// translate segments to scanned reference
				List<QueryAlignedSegment> queryToScannedRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, queryToTipAlmtRefSegs, refToScan);
				
				// trim query to scanned ref segs down to the feature area.
				List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
	
				List<QueryAlignedSegment> queryToScannedRefSegs = 
						ReferenceSegment.intersection(queryToScannedRefSegsFull, featureLocRefSegs, ReferenceSegment.cloneLeftSegMerger());
	
				String fastaNTs = fastaNTSeq.getSequenceAsString();
	
				List<NtQueryAlignedSegment> queryToScannedRefNtSegs =
						queryToScannedRefSegs.stream()
						.map(seg -> new NtQueryAlignedSegment(seg.getRefStart(), seg.getRefEnd(), seg.getQueryStart(), seg.getQueryEnd(),
								SegmentUtils.base1SubString(fastaNTs, seg.getQueryStart(), seg.getQueryEnd())))
								.collect(Collectors.toList());
	
	
				variationScanResults.addAll(featureLoc.variationScan(cmdContext, queryToScannedRefNtSegs, variationsToScan, excludeAbsent));
			}
		}

		VariationScanResult.sortVariationScanResults(variationScanResults);
		return variationScanResults;
	}

	@CompleterClass
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {}
	
}
