package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils;

import gnu.trove.map.TIntCharMap;
import gnu.trove.map.hash.TIntCharHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SimpleNucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.AllColumnsAlignment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegmentTree;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.CodonLabelAnnotation;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.DetailAnnotation;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.DetailAnnotationRow;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.DetailAnnotationSegment;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.FeatureVisualisation;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.QueryAaContentAnnotation;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.QueryNtContentAnnotation;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.QueryNtIndexAnnotation;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.RefAaContentAnnotation;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.RefNtContentAnnotation;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.RefNtIndexAnnotation;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.VisualisationAnnotationRow;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(
		commandWords={"visualise-feature"}, 
		description = "Produce feature visualisation document based on pairwise alignment with target reference", 
		docoptUsages = { },
		furtherHelp = "Given query-aligned segments between some query sequence and a target reference sequence, "
				+ "and nucleotide content for the query sequence, produce a document for visualising the "
				+ "specified feature in both the query and a 'comparison' reference, with an integrated "
				+ "coordinate 'u-space', allowing indels. 'Details' marking up the query sequence may also be supplied, "
				+ "these are returned, transformed into the integrated 'u-space'."
				,
		metaTags = { CmdMeta.inputIsComplex }
)
public class VisualiseFeatureCommand extends ModulePluginCommand<PojoCommandResult<FeatureVisualisation>, VisualisationUtility> {

	private static final String TARGET_REFERENCE_NAME = "targetReferenceName";
	private static final String COMPARISON_REFERENCE_NAME = "comparisonReferenceName";
	private static final String FEATURE_NAME = "featureName";
	private static final String QUERY_TO_TARGET_REF_SEGMENTS = "queryToTargetRefSegments";
	private static final String QUERY_NUCLEOTIDES = "queryNucleotides";
	private static final String QUERY_DETAILS = "queryDetails";

	private String targetReferenceName;
	private String comparisonReferenceName;
	private String featureName;
	private List<QueryAlignedSegment> queryToTargetRefSegments = new ArrayList<QueryAlignedSegment>();
	private String queryNucleotides;
	private List<Detail> queryDetails;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.targetReferenceName = PluginUtils.configureStringProperty(configElem, TARGET_REFERENCE_NAME, true);
		this.comparisonReferenceName = PluginUtils.configureStringProperty(configElem, COMPARISON_REFERENCE_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		List<Element> queryToTargetRefSegElems = PluginUtils.findConfigElements(configElem, QUERY_TO_TARGET_REF_SEGMENTS);
		this.queryToTargetRefSegments = PluginFactory.createPlugins(pluginConfigContext, QueryAlignedSegment.class, queryToTargetRefSegElems);
		this.queryNucleotides = PluginUtils.configureStringProperty(configElem, QUERY_NUCLEOTIDES, true);
		List<Element> queryDetailElems = PluginUtils.findConfigElements(configElem, QUERY_DETAILS);
		this.queryDetails = PluginFactory.createPlugins(pluginConfigContext, Detail.class, queryDetailElems);
	}


	@Override
	protected PojoCommandResult<FeatureVisualisation> execute(CommandContext cmdContext, VisualisationUtility modulePlugin) {
		
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(this.comparisonReferenceName, this.featureName));
		ReferenceSequence comparisonReference = featureLoc.getReferenceSequence();
		String referenceNucleotides = 
				comparisonReference.getSequence().getSequenceObject().getNucleotides(cmdContext);

		ReferenceSequence targetReference = 
				GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetReferenceName));
		
		Translator translator = new CommandContextTranslator(cmdContext);

		
		// initial alignment uspace includes just the reference sequence.
		int refLength = referenceNucleotides.length();
		AllColumnsAlignment<String> allColumnsAlmt = new AllColumnsAlignment<String>("reference", refLength);

		String linkingAlmtName = modulePlugin.getLinkingAlignmentName();
		Alignment linkingAlmt = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(linkingAlmtName));

		AlignmentMember targetRefLinkingAlmtMember = targetReference.getLinkingAlignmentMembership(linkingAlmtName);
		List<QueryAlignedSegment> targetRefToLinkingAlmt = targetRefLinkingAlmtMember.segmentsAsQueryAlignedSegments();
		
		List<QueryAlignedSegment> targetRefToComparisonRef = 
				linkingAlmt.translateToRelatedRef(cmdContext, targetRefToLinkingAlmt, comparisonReference);
		
		List<QueryAlignedSegment> queryToComparisonRef = QueryAlignedSegment
				.translateSegments(queryToTargetRefSegments, targetRefToComparisonRef);
		
		allColumnsAlmt.addRow("query", "reference", queryToComparisonRef, queryNucleotides.length());

		allColumnsAlmt.rationalise();

		// "display" alignment will be region corresponding to the named feature location.
		List<QueryAlignedSegment> refToUSegs = allColumnsAlmt.getSegments("reference");
		List<QueryAlignedSegment> uToRefSegs = QueryAlignedSegment.invertList(refToUSegs);
		
		List<ReferenceSegment> featureLocSegs = featureLoc.segmentsAsReferenceSegments();
		List<QueryAlignedSegment> uToRefFeatureLocSegs = ReferenceSegment.intersection(uToRefSegs, featureLocSegs,
				ReferenceSegment.cloneLeftSegMerger());

		// offset required to shift "U" NT coordinates to display coordinates so that the named feature starts at display location 1.
		int displayNtOffset = QueryAlignedSegment.minQueryStart(uToRefFeatureLocSegs)-1;

		// calculate width of display alignment
		int displayNtWidth = QueryAlignedSegment.maxQueryEnd(uToRefFeatureLocSegs)-displayNtOffset;

		char[] refNtsInUSpace = new char[displayNtWidth];
		TIntCharMap refNtToRefAa = new TIntCharHashMap();
		
		VisualisationAnnotationRow<RefNtContentAnnotation> refNtContentRow = new VisualisationAnnotationRow<RefNtContentAnnotation>();
		refNtContentRow.annotationType = "refNtContent";
		VisualisationAnnotationRow<RefNtIndexAnnotation> refNtIndexRow = new VisualisationAnnotationRow<RefNtIndexAnnotation>();
		refNtIndexRow.annotationType = "refNtIndex";

		List<QueryAlignedSegment> refToUFeatureLocSegs = QueryAlignedSegment.invertList(uToRefFeatureLocSegs);

		refToUFeatureLocSegs.forEach(seg -> {
			RefNtContentAnnotation refNtContent = new RefNtContentAnnotation();
			refNtContent.displayNtPos = seg.getRefStart()-displayNtOffset;
			refNtContent.ntContent = FastaUtils.subSequence(referenceNucleotides, seg.getQueryStart(), seg.getQueryEnd()).toString();
			refNtContentRow.annotations.add(refNtContent);
			RefNtIndexAnnotation refStartAnnotation = new RefNtIndexAnnotation();
			refStartAnnotation.displayNtPos = seg.getRefStart()-displayNtOffset;
			refStartAnnotation.ntIndex = seg.getQueryStart();
			refStartAnnotation.endOfSegment = true;
			refNtIndexRow.annotations.add(refStartAnnotation);
			
			for(int ntIndex = seg.getQueryStart()+1; ntIndex < seg.getQueryEnd(); ntIndex++) {
				if(ntIndex % 10 == 0) {
					RefNtIndexAnnotation regularAnnotation = new RefNtIndexAnnotation();
					regularAnnotation.displayNtPos = (ntIndex+seg.getQueryToReferenceOffset())-displayNtOffset;
					regularAnnotation.ntIndex = ntIndex;
					regularAnnotation.endOfSegment = false;
					refNtIndexRow.annotations.add(regularAnnotation);
				}
			}
			
			RefNtIndexAnnotation refEndAnnotation = new RefNtIndexAnnotation();
			refEndAnnotation.displayNtPos = seg.getRefEnd()-displayNtOffset;
			refEndAnnotation.ntIndex = seg.getQueryEnd();
			refEndAnnotation.endOfSegment = true;
			refNtIndexRow.annotations.add(refEndAnnotation);
			int uIndex = seg.getRefStart()-displayNtOffset;
			for(int refNtIndex = seg.getQueryStart(); refNtIndex <= seg.getQueryEnd(); refNtIndex++) {
				refNtsInUSpace[uIndex-1] = FastaUtils.nt(referenceNucleotides, refNtIndex);
				uIndex++;
			}
		});


		int uFeatureStart = ReferenceSegment.minRefStart(refToUFeatureLocSegs);
		int uFeatureEnd = ReferenceSegment.maxRefEnd(refToUFeatureLocSegs);
		
		List<QueryAlignedSegment> queryToUSegs = allColumnsAlmt.getSegments("query");

		List<QueryAlignedSegment> queryToUFeatureLocSegs = 
				ReferenceSegment.intersection(queryToUSegs, Arrays.asList(new ReferenceSegment(uFeatureStart, uFeatureEnd)), 
						ReferenceSegment.cloneLeftSegMerger());

		VisualisationAnnotationRow<QueryNtContentAnnotation> queryNtContentRow = new VisualisationAnnotationRow<QueryNtContentAnnotation>();
		queryNtContentRow.annotationType = "queryNtContent";
		VisualisationAnnotationRow<QueryNtIndexAnnotation> queryNtIndexRow = new VisualisationAnnotationRow<QueryNtIndexAnnotation>();
		queryNtIndexRow.annotationType = "queryNtIndex";
		queryToUFeatureLocSegs.forEach(seg -> {
			QueryNtContentAnnotation queryNtContent = new QueryNtContentAnnotation();
			queryNtContent.displayNtPos = seg.getRefStart()-displayNtOffset;
			queryNtContent.ntContent = FastaUtils.subSequence(queryNucleotides, seg.getQueryStart(), seg.getQueryEnd()).toString();
			int uIndex = seg.getRefStart()-displayNtOffset;
			int displayPos = queryNtContent.displayNtPos;
			for(int queryNtIndex = seg.getQueryStart(); queryNtIndex <= seg.getQueryEnd(); queryNtIndex++) {
				char refNt = refNtsInUSpace[uIndex-1];
				char queryNt = FastaUtils.nt(queryNucleotides, queryNtIndex);
				if(refNt != 0 && refNt != queryNt) {
					queryNtContent.ntDisplayPosDifferences.add(displayPos);
				}
				uIndex++;
				displayPos++;
			}
			
			
			queryNtContentRow.annotations.add(queryNtContent);
			QueryNtIndexAnnotation queryStartAnnotation = new QueryNtIndexAnnotation();
			queryStartAnnotation.displayNtPos = seg.getRefStart()-displayNtOffset;
			queryStartAnnotation.ntIndex = seg.getQueryStart();
			queryStartAnnotation.endOfSegment = true;
			queryNtIndexRow.annotations.add(queryStartAnnotation);
			
			for(int ntIndex = seg.getQueryStart()+1; ntIndex < seg.getQueryEnd(); ntIndex++) {
				if(ntIndex % 10 == 0) {
					QueryNtIndexAnnotation regularAnnotation = new QueryNtIndexAnnotation();
					regularAnnotation.displayNtPos = (ntIndex+seg.getQueryToReferenceOffset())-displayNtOffset;
					regularAnnotation.ntIndex = ntIndex;
					regularAnnotation.endOfSegment = false;
					queryNtIndexRow.annotations.add(regularAnnotation);
				}
			}
			
			QueryNtIndexAnnotation queryEndAnnotation = new QueryNtIndexAnnotation();
			queryEndAnnotation.displayNtPos = seg.getRefEnd()-displayNtOffset;
			queryEndAnnotation.ntIndex = seg.getQueryEnd();
			queryEndAnnotation.endOfSegment = true;
			queryNtIndexRow.annotations.add(queryEndAnnotation);
		});

		VisualisationAnnotationRow<CodonLabelAnnotation> codonLabelRow = null;
		VisualisationAnnotationRow<RefAaContentAnnotation> refAaRow = null;
		VisualisationAnnotationRow<QueryAaContentAnnotation> queryAaRow = null;
		
		if(featureLoc.getFeature().codesAminoAcids()) {
			codonLabelRow = new VisualisationAnnotationRow<>();
			codonLabelRow.annotationType = "codonLabel";

			refAaRow = new VisualisationAnnotationRow<>();
			refAaRow.annotationType = "refAa";

			List<LabeledQueryAminoAcid> refLqaas = featureLoc.getReferenceAminoAcidContent(cmdContext);
			for(LabeledQueryAminoAcid refLqaa: refLqaas) {
				LabeledAminoAcid labeledAminoAcid = refLqaa.getLabeledAminoAcid();

				int refNt = refLqaa.getQueryNtStart();
				QueryAlignedSegment qaSeg = new QueryAlignedSegment(refNt, refNt, refNt, refNt);
				int displayNtPos = QueryAlignedSegment.translateSegments(Arrays.asList(qaSeg), refToUSegs).get(0).getRefStart()-displayNtOffset;
				
				LabeledCodon labeledCodon = labeledAminoAcid.getLabeledCodon();
				CodonLabelAnnotation codonLabelAnnotation = new CodonLabelAnnotation();
				codonLabelAnnotation.label = labeledCodon.getCodonLabel();
				codonLabelAnnotation.ntWidth = labeledCodon.getNtLength();
				codonLabelAnnotation.displayNtPos = displayNtPos;
				codonLabelRow.annotations.add(codonLabelAnnotation);

				RefAaContentAnnotation refAaContentAnnotation = new RefAaContentAnnotation();
				refAaContentAnnotation.aa = labeledAminoAcid.getAminoAcid();
				refAaContentAnnotation.ntWidth = labeledCodon.getNtLength();
				refAaContentAnnotation.displayNtPos = displayNtPos;
				refAaRow.annotations.add(refAaContentAnnotation);
				refNtToRefAa.put(labeledCodon.getNtStart(), refAaContentAnnotation.aa.charAt(0));
			}
			queryAaRow = new VisualisationAnnotationRow<QueryAaContentAnnotation>();
			queryAaRow.annotationType = "queryAa";

			List<LabeledQueryAminoAcid> queryLqaas = 
					featureLoc.translateQueryNucleotides(cmdContext, translator, queryToComparisonRef, 
							new SimpleNucleotideContentProvider(queryNucleotides));
			
			for(LabeledQueryAminoAcid queryLqaa: queryLqaas) {
				LabeledAminoAcid labeledAminoAcid = queryLqaa.getLabeledAminoAcid();

				int queryNt = queryLqaa.getQueryNtStart();
				QueryAlignedSegment qaSeg = new QueryAlignedSegment(queryNt, queryNt, queryNt, queryNt);
				int displayNtPos = QueryAlignedSegment.translateSegments(Arrays.asList(qaSeg), queryToUSegs).get(0).getRefStart()-displayNtOffset;

				LabeledCodon labeledCodon = labeledAminoAcid.getLabeledCodon();
				QueryAaContentAnnotation queryAaContentAnnotation = new QueryAaContentAnnotation();
				queryAaContentAnnotation.aa = labeledAminoAcid.getAminoAcid();
				
				queryAaContentAnnotation.ntWidth = labeledCodon.getNtLength();
				queryAaContentAnnotation.displayNtPos = displayNtPos;
				
				String definiteAasString = labeledAminoAcid.getTranslationInfo().getDefiniteAasString();
				if(definiteAasString.length() > 1) {
					queryAaContentAnnotation.multipleAas = Arrays.asList(definiteAasString.split(""));
				}
				queryAaRow.annotations.add(queryAaContentAnnotation);
				
				char refAa = refNtToRefAa.get(labeledCodon.getNtStart());
				if(refAa != 0 && refAa != queryAaContentAnnotation.aa.charAt(0)) {
					queryAaContentAnnotation.differentFromRef = true;
				}
				
			}
		
		}

		FeatureVisualisation featureVisualisation = new FeatureVisualisation();
		featureVisualisation.comparisonReferenceName = comparisonReference.getName();
		featureVisualisation.comparisonReferenceDisplayName = comparisonReference.getRenderedName();
		featureVisualisation.featureName = featureLoc.getFeature().getName();
		featureVisualisation.featureDisplayName = featureLoc.getFeature().getRenderedName();
		featureVisualisation.displayNtWidth = displayNtWidth;
		if(codonLabelRow != null) {
			featureVisualisation.annotationRows.add(codonLabelRow);
		}
		if(refAaRow != null) {
			featureVisualisation.annotationRows.add(refAaRow);
		}
		featureVisualisation.annotationRows.add(refNtContentRow);
		featureVisualisation.annotationRows.add(refNtIndexRow);
		if(queryAaRow != null) {
			featureVisualisation.annotationRows.add(queryAaRow);
		}
		featureVisualisation.annotationRows.add(queryNtContentRow);
		featureVisualisation.annotationRows.add(queryNtIndexRow);
		
		List<DetailAnnotation> detailAnnotations = new ArrayList<DetailAnnotation>();
		for(Detail detail: queryDetails) {
			String detailId = detail.getId();
			
			DetailAnnotation detailAnnotation = new DetailAnnotation();
			detailAnnotation.detailId = detailId;
			for(DetailSegment detailSegment: detail.getDetailSegments()) {
				String segmentId = detailSegment.getId();
				QueryAlignedSegment detailSegmentToSelf = 
						new QueryAlignedSegment(detailSegment.getRefStart(), detailSegment.getRefEnd(), 
								detailSegment.getRefStart(), detailSegment.getRefEnd());
				List<QueryAlignedSegment> detailSegmentsToU = 
						QueryAlignedSegment.translateSegments(Arrays.asList(detailSegmentToSelf), queryToUFeatureLocSegs);
				if(detailSegmentsToU.isEmpty()) {
					continue;
				}
				for(QueryAlignedSegment detailSegmentToU: detailSegmentsToU) {
					DetailAnnotationSegment detailAnnotationSegment = new DetailAnnotationSegment();
					detailAnnotationSegment.segmentId = segmentId;
					detailAnnotationSegment.displayNtStart = detailSegmentToU.getRefStart() - displayNtOffset;
					detailAnnotationSegment.displayNtEnd = detailSegmentToU.getRefEnd() - displayNtOffset;
					detailAnnotation.segments.add(detailAnnotationSegment);
				}
				detailAnnotation.minRefStart = ReferenceSegment.minRefStart(detailAnnotation.segments);
				detailAnnotation.maxRefEnd = ReferenceSegment.maxRefEnd(detailAnnotation.segments);
			}
			if(!detailAnnotation.segments.isEmpty()) {
				detailAnnotations.add(detailAnnotation);
			}
		}
		List<ReferenceSegmentTree<DetailAnnotation>> trackTrees = new ArrayList<ReferenceSegmentTree<DetailAnnotation>>();
		for(DetailAnnotation detailAnnotation: detailAnnotations) {
			boolean addedToTrack = false;
			for(ReferenceSegmentTree<DetailAnnotation> trackTree: trackTrees) {
				List<DetailAnnotation> overlapping = new ArrayList<DetailAnnotation>();
				trackTree.findOverlapping(detailAnnotation.getRefStart(), detailAnnotation.getRefEnd(), overlapping);
				if(overlapping.isEmpty()) {
					trackTree.add(detailAnnotation);
					addedToTrack = true;
					break;
				}
			}
			if(!addedToTrack) {
				ReferenceSegmentTree<DetailAnnotation> newTrackTree = new ReferenceSegmentTree<DetailAnnotation>();
				newTrackTree.add(detailAnnotation);
				trackTrees.add(newTrackTree);
			}
		}
		for(int i = 0; i < trackTrees.size(); i++) {
			DetailAnnotationRow detailAnnotationRow = new DetailAnnotationRow();
			detailAnnotationRow.annotationType = "detail";
			detailAnnotationRow.trackNumber = i;
			trackTrees.get(i).findOverlapping(1, displayNtWidth, detailAnnotationRow.annotations);
			featureVisualisation.annotationRows.add(detailAnnotationRow);
		}		
		return new PojoCommandResult<FeatureVisualisation>(featureVisualisation);
		
	}
	
}
