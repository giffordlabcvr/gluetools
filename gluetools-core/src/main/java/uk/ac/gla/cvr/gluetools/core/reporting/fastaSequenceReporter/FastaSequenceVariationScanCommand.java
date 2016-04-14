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
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a FASTA file for variations", 
		docoptUsages = { "-i <fileName> -r <acRefName> [-m] -f <featureName> [-t <targetRefName>] [-a <tipAlmtName>] [-w <whereClause>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 FASTA input file",
				"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
				"-m, --multiReference                                 Scan across references",
				"-f <featureName>, --featureName <featureName>        Feature to translate",
				"-t <targetRefName>, --targetRefName <targetRefName>  Target reference",
				"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
				"-w <whereClause>, --whereClause <whereClause>        Qualify variations",
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
				"The variation scan will be limited to the specified feature location. "+
				"If <whereClause> is used, this qualifies the set of variations which are scanned for.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class FastaSequenceVariationScanCommand extends FastaSequenceReporterCommand<FastaSequenceVariationScanResult> 
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
	protected FastaSequenceVariationScanResult execute(CommandContext cmdContext,
			FastaSequenceReporter fastaSequenceReporter) {
		// check feature exists.
		GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(getFeatureName()));

		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		
		Entry<String, DNASequence> fastaEntry = getFastaEntry(consoleCmdContext);
		String fastaID = fastaEntry.getKey();
		DNASequence fastaNTSeq = fastaEntry.getValue();

		String targetRefName = Optional.ofNullable(getTargetRefName())
				.orElse(fastaSequenceReporter.targetRefNameFromFastaId(consoleCmdContext, fastaID));
		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));

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

		List<VariationScanResult> variationScanResults = new ArrayList<VariationScanResult>();
		
		for(ReferenceSequence refToScan: refsToScan) {

			FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(refToScan.getName(), getFeatureName()), true);
			if(featureLoc == null) {
				continue;
			}
			
			List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, whereClause);
			if(variationsToScan.isEmpty()) {
				continue;
			}

			// align query to target reference
			Aligner<?, ?> aligner = Aligner.getAligner(cmdContext, fastaSequenceReporter.getAlignerModuleName());
			AlignerResult alignerResult = aligner.doAlign(cmdContext, targetRef.getName(), fastaID, fastaNTSeq);

			// extract segments from aligner result
			List<QueryAlignedSegment> queryToTargetRefSegs = alignerResult.getQueryIdToAlignedSegments().get(fastaID);

			// translate segments to tip alignment reference
			List<QueryAlignedSegment> queryToTipAlmtRefSegs = tipAlmt.translateToRef(cmdContext, 
					tipAlmtMember.getSequence().getSource().getName(), tipAlmtMember.getSequence().getSequenceID(), 
					queryToTargetRefSegs);

			// translate segments to ancestor constraining reference
			List<QueryAlignedSegment> queryToAncConstrRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, queryToTipAlmtRefSegs, refToScan);


			// trim down to the feature area.
			List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();

			List<QueryAlignedSegment> queryToAncConstrRefSegs = 
					ReferenceSegment.intersection(queryToAncConstrRefSegsFull, featureLocRefSegs, ReferenceSegment.cloneLeftSegMerger());

			String fastaNTs = fastaNTSeq.getSequenceAsString();

			List<NtQueryAlignedSegment> queryToAncConstrRefNtSegs =
					queryToAncConstrRefSegs.stream()
					.map(seg -> new NtQueryAlignedSegment(seg.getRefStart(), seg.getRefEnd(), seg.getQueryStart(), seg.getQueryEnd(),
							SegmentUtils.base1SubString(fastaNTs, seg.getQueryStart(), seg.getQueryEnd())))
							.collect(Collectors.toList());


			variationScanResults.addAll(featureLoc.variationScan(cmdContext, queryToAncConstrRefNtSegs, variationsToScan));

		}

		VariationScanResult.sortVariationScanResults(variationScanResults);
		
		return new FastaSequenceVariationScanResult(variationScanResults);
	}

	@CompleterClass
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {}
	
}
