package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.Collections;
import java.util.Comparator;
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
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(
		commandWords={"variation", "scan"}, 
		description = "Scan a FASTA file for variations", 
		docoptUsages = { "-i <fileName> -r <acRefName> -f <featureName> -t <targetRefName> [-a <tipAlmtName>] [-w <whereClause>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 FASTA input file",
				"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
				"-f <featureName>, --featureName <featureName>        Feature to translate",
				"-t <targetRefName>, --targetRefName <targetRefName>  Target reference",
				"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
				"-w <whereClause>, --whereClause <whereClause>        Qualify variations",
		},
		furtherHelp = 
		        "This command aligns a FASTA query sequence to a 'target' reference sequence, and "+
		        "scans a section of the query sequence for variations based on the target reference sequence's "+
				"place in the alignment tree. The target reference sequence must be a member of a constrained "+
		        "'tip alignment'. The tip alignment may be specified by <tipAlmtName>. If unspecified, it will be "+
		        "inferred from the target reference if possible. "+
		        "The <acRefName> argument specifies an 'ancestor-constraining' reference sequence. "+
				"This must be the constraining reference of an ancestor alignment of the tip alignment. "+
				"The <featureName> arguments specifies a feature location on the ancestor-constraining reference. "+
				"The variation scan will be limited to the specified feature location. "+
				"If <whereClause> is used, this qualifies the set of variations which are scanned for.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class FastaSequenceVariationScanCommand extends ModulePluginCommand<FastaSequenceVariationScanResult, FastaSequenceReporter> 
	implements ProvidedProjectModeCommand{

	public static final String FILE_NAME = "fileName";

	public static final String AC_REF_NAME = "acRefName";
	public static final String FEATURE_NAME = "featureName";
	
	public static final String TARGET_REF_NAME = "targetRefName";
	public static final String TIP_ALMT_NAME = "tipAlmtName";

	public static final String WHERE_CLAUSE = "whereClause";

	private String fileName;
	private String acRefName;
	private String featureName;
	private String tipAlmtName;
	private String targetRefName;
	private Expression whereClause;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.acRefName = PluginUtils.configureStringProperty(configElem, AC_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.targetRefName = PluginUtils.configureStringProperty(configElem, TARGET_REF_NAME, true);
		this.tipAlmtName = PluginUtils.configureStringProperty(configElem, TIP_ALMT_NAME, false);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);

		super.configure(pluginConfigContext, configElem);
	}

	@Override
	protected FastaSequenceVariationScanResult execute(CommandContext cmdContext,
			FastaSequenceReporter fastaSequenceReporter) {
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(acRefName, featureName), false);

		List<Variation> variationsToScan = featureLoc.getVariationsQualified(cmdContext, whereClause);
		
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		
		Entry<String, DNASequence> fastaEntry = getFastaEntry(consoleCmdContext);
		String fastaID = fastaEntry.getKey();
		DNASequence fastaNTSeq = fastaEntry.getValue();

		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));

		AlignmentMember tipAlmtMember = targetRef.getConstrainedAlignmentMembership(tipAlmtName);
		Alignment tipAlmt = tipAlmtMember.getAlignment();

		ReferenceSequence ancConstrainingRef = tipAlmt.getAncConstrainingRef(cmdContext, acRefName);

		
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
		List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
		
		List<QueryAlignedSegment> queryToAncConstrRefSegs = 
					ReferenceSegment.intersection(queryToAncConstrRefSegsFull, featureLocRefSegs, ReferenceSegment.cloneLeftSegMerger());

		String fastaNTs = fastaNTSeq.getSequenceAsString();

		List<NtQueryAlignedSegment> queryToAncConstrRefNtSegs =
				queryToAncConstrRefSegs.stream()
				.map(seg -> new NtQueryAlignedSegment(seg.getRefStart(), seg.getRefEnd(), seg.getQueryStart(), seg.getQueryEnd(),
						SegmentUtils.base1SubString(fastaNTs, seg.getQueryStart(), seg.getQueryEnd())))
				.collect(Collectors.toList());

		
		List<VariationScanResult> variationScanResults = featureLoc.variationScan(cmdContext, queryToAncConstrRefNtSegs, variationsToScan);
		VariationScanResult.sortVariationScanResults(variationScanResults);
		
		return new FastaSequenceVariationScanResult(variationScanResults);
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
	public static class Completer extends FastaSequenceAminoAcidCommand.Completer {}
	
}
