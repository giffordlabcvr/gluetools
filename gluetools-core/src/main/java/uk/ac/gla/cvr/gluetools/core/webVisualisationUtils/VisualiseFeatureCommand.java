package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils;

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
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.FeatureLocBaseAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.AllColumnsAlignment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos.CodonLabelAnnotation;
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
		description = "Produce feature visualisation document based on query-aligned segments", 
		docoptUsages = { },
		furtherHelp = "Given query-aligned segments between some query sequence and a reference sequence, "
				+ "and nucleotide content for the query sequence, produce a document for visualising the "
				+ "specified feature in the query.",
		metaTags = { CmdMeta.inputIsComplex }
)
public class VisualiseFeatureCommand extends ModulePluginCommand<PojoCommandResult<FeatureVisualisation>, VisualisationUtility> {

	private static final String REFERENCE_NAME = "referenceName";
	private static final String FEATURE_NAME = "featureName";
	private static final String QUERY_TO_REF_SEGMENTS = "queryToRefSegments";
	private static final String QUERY_NUCLEOTIDES = "queryNucleotides";

	private String referenceName;
	private String featureName;
	private List<QueryAlignedSegment> queryToRefSegments;
	private String queryNucleotides;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		List<Element> qaSegElems = PluginUtils.findConfigElements(configElem, QUERY_TO_REF_SEGMENTS, null, null);
		this.queryToRefSegments = PluginFactory.createPlugins(pluginConfigContext, QueryAlignedSegment.class, qaSegElems);
		this.referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.queryNucleotides = PluginUtils.configureStringProperty(configElem, QUERY_NUCLEOTIDES, true);
	}


	@Override
	protected PojoCommandResult<FeatureVisualisation> execute(CommandContext cmdContext, VisualisationUtility modulePlugin) {
		
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(this.referenceName, this.featureName));
		List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
		
		int minRefStart = ReferenceSegment.minRefStart(featureLocRefSegs);
		int maxRefEnd = ReferenceSegment.maxRefEnd(featureLocRefSegs);
		
		AllColumnsAlignment<String> allColumnsAlmt = new AllColumnsAlignment<String>("reference", maxRefEnd);

		// trim qa segs down to the feature area.
		List<QueryAlignedSegment> queryToFeatureLocRefSegs = ReferenceSegment.intersection(queryToRefSegments, featureLocRefSegs,
				ReferenceSegment.cloneLeftSegMerger());
		allColumnsAlmt.addRow("query", "reference", queryToFeatureLocRefSegs, QueryAlignedSegment.maxQueryEnd(queryToFeatureLocRefSegs));
		
		allColumnsAlmt.rationalise();
		
		int utNtWidth = allColumnsAlmt.getMaxIndex() - (minRefStart - 1);
		List<ReferenceSegment> uFeatureSegs = Arrays.asList(new ReferenceSegment(minRefStart, allColumnsAlmt.getMaxIndex()));

		String referenceNucleotides = featureLoc.getReferenceSequence().getSequence().getSequenceObject().getNucleotides(cmdContext);
		
		List<QueryAlignedSegment> refToUFeatureSegs = allColumnsAlmt.getSegments("reference");
		refToUFeatureSegs = ReferenceSegment.intersection(refToUFeatureSegs, uFeatureSegs, ReferenceSegment.cloneLeftSegMerger());
		refToUFeatureSegs.forEach(seg -> seg.translateRef(- (minRefStart - 1)));
		VisualisationAnnotationRow refNtContentRow = new VisualisationAnnotationRow();
		refNtContentRow.annotationType = "refNtContent";
		VisualisationAnnotationRow refNtIndexRow = new VisualisationAnnotationRow();
		refNtIndexRow.annotationType = "refNtIndex";
		refToUFeatureSegs.forEach(seg -> {
			RefNtContentAnnotation refNtContent = new RefNtContentAnnotation();
			refNtContent.uNtPos = seg.getRefStart();
			refNtContent.ntContent = FastaUtils.subSequence(referenceNucleotides, seg.getQueryStart(), seg.getQueryEnd()).toString();
			refNtContentRow.annotations.add(refNtContent);
			RefNtIndexAnnotation refStartAnnotation = new RefNtIndexAnnotation();
			refStartAnnotation.uNtPos = seg.getRefStart();
			refStartAnnotation.ntIndex = seg.getQueryStart();
			refNtIndexRow.annotations.add(refStartAnnotation);
			RefNtIndexAnnotation refEndAnnotation = new RefNtIndexAnnotation();
			refEndAnnotation.uNtPos = seg.getRefEnd();
			refEndAnnotation.ntIndex = seg.getQueryEnd();
			refNtIndexRow.annotations.add(refEndAnnotation);
		});
		
		List<QueryAlignedSegment> queryToUFeatureSegs = allColumnsAlmt.getSegments("query");
		queryToUFeatureSegs = ReferenceSegment.intersection(queryToUFeatureSegs, uFeatureSegs, ReferenceSegment.cloneLeftSegMerger());
		queryToUFeatureSegs.forEach(seg -> seg.translateRef(- (minRefStart - 1)));
		VisualisationAnnotationRow queryNtContentRow = new VisualisationAnnotationRow();
		queryNtContentRow.annotationType = "queryNtContent";
		VisualisationAnnotationRow queryNtIndexRow = new VisualisationAnnotationRow();
		queryNtIndexRow.annotationType = "queryNtIndex";
		queryToUFeatureSegs.forEach(seg -> {
			QueryNtContentAnnotation queryNtContent = new QueryNtContentAnnotation();
			queryNtContent.uNtPos = seg.getRefStart();
			queryNtContent.ntContent = FastaUtils.subSequence(queryNucleotides, seg.getQueryStart(), seg.getQueryEnd()).toString();
			queryNtContentRow.annotations.add(queryNtContent);
			QueryNtIndexAnnotation queryStartAnnotation = new QueryNtIndexAnnotation();
			queryStartAnnotation.uNtPos = seg.getRefStart();
			queryStartAnnotation.ntIndex = seg.getQueryStart();
			queryNtIndexRow.annotations.add(queryStartAnnotation);
			QueryNtIndexAnnotation queryEndAnnotation = new QueryNtIndexAnnotation();
			queryEndAnnotation.uNtPos = seg.getRefEnd();
			queryEndAnnotation.ntIndex = seg.getQueryEnd();
			queryNtIndexRow.annotations.add(queryEndAnnotation);
		});
		

		VisualisationAnnotationRow codonLabelRow = null;
		VisualisationAnnotationRow refAaRow = null;
		VisualisationAnnotationRow queryAaRow = null;
		
		if(featureLoc.getFeature().codesAminoAcids()) {
			codonLabelRow = new VisualisationAnnotationRow();
			codonLabelRow.annotationType = "codonLabel";

			refAaRow = new VisualisationAnnotationRow();
			refAaRow.annotationType = "refAa";

			List<LabeledQueryAminoAcid> refLqaas = FeatureLocBaseAminoAcidCommand.featureLocAminoAcids(cmdContext, featureLoc);
			for(LabeledQueryAminoAcid refLqaa: refLqaas) {
				LabeledAminoAcid labeledAminoAcid = refLqaa.getLabeledAminoAcid();

				int refNt = refLqaa.getQueryNt();
				QueryAlignedSegment qaSeg = new QueryAlignedSegment(refNt, refNt, refNt, refNt);
				int uNtPos = QueryAlignedSegment.translateSegments(Arrays.asList(qaSeg), refToUFeatureSegs).get(0).getRefStart();
				
				LabeledCodon labeledCodon = labeledAminoAcid.getLabeledCodon();
				CodonLabelAnnotation codonLabelAnnotation = new CodonLabelAnnotation();
				codonLabelAnnotation.label = labeledCodon.getCodonLabel();
				codonLabelAnnotation.ntWidth = labeledCodon.getNtLength();
				codonLabelAnnotation.uNtPos = uNtPos;
				codonLabelRow.annotations.add(codonLabelAnnotation);

				RefAaContentAnnotation refAaContentAnnotation = new RefAaContentAnnotation();
				refAaContentAnnotation.aa = labeledAminoAcid.getAminoAcid();
				refAaContentAnnotation.ntWidth = labeledCodon.getNtLength();
				refAaContentAnnotation.uNtPos = uNtPos;
				refAaRow.annotations.add(refAaContentAnnotation);
			}
			
			queryAaRow = new VisualisationAnnotationRow();
			queryAaRow.annotationType = "queryAa";

			List<LabeledQueryAminoAcid> queryLqaas = TranslationUtils
					.translateQaSegments(cmdContext, featureLoc.getReferenceSequence(), featureName, queryToRefSegments, queryNucleotides);
			for(LabeledQueryAminoAcid queryLqaa: queryLqaas) {
				LabeledAminoAcid labeledAminoAcid = queryLqaa.getLabeledAminoAcid();

				int queryNt = queryLqaa.getQueryNt();
				QueryAlignedSegment qaSeg = new QueryAlignedSegment(queryNt, queryNt, queryNt, queryNt);
				int uNtPos = QueryAlignedSegment.translateSegments(Arrays.asList(qaSeg), queryToUFeatureSegs).get(0).getRefStart();

				LabeledCodon labeledCodon = labeledAminoAcid.getLabeledCodon();
				QueryAaContentAnnotation queryAaContentAnnotation = new QueryAaContentAnnotation();
				queryAaContentAnnotation.aa = labeledAminoAcid.getAminoAcid();
				queryAaContentAnnotation.ntWidth = labeledCodon.getNtLength();
				queryAaContentAnnotation.uNtPos = uNtPos;
				queryAaRow.annotations.add(queryAaContentAnnotation);
			}
		
		}

		FeatureVisualisation featureVisualisation = new FeatureVisualisation();
		featureVisualisation.referenceName = featureLoc.getReferenceSequence().getName();
		featureVisualisation.referenceDisplayName = featureLoc.getReferenceSequence().getRenderedName();
		featureVisualisation.featureName = featureLoc.getFeature().getName();
		featureVisualisation.featureDisplayName = featureLoc.getFeature().getRenderedName();
		featureVisualisation.uNtWidth = utNtWidth;
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

		
		return new PojoCommandResult<FeatureVisualisation>(featureVisualisation);
		
	}
	
}
