package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils;

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
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SimpleNucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.AllColumnsAlignment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
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
	private List<QueryAlignedSegment> queryToRefSegments = new ArrayList<QueryAlignedSegment>();
	private String queryNucleotides;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		CommandDocument qaSegsCmdDoc = PluginUtils.configureCommandDocumentProperty(configElem, QUERY_TO_REF_SEGMENTS, true);
		qaSegsCmdDoc.getArray("alignedSegment").getItems().forEach(item -> {
			queryToRefSegments.add(new QueryAlignedSegment((CommandObject) item));
		});
		this.referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.queryNucleotides = PluginUtils.configureStringProperty(configElem, QUERY_NUCLEOTIDES, true);
	}


	@Override
	protected PojoCommandResult<FeatureVisualisation> execute(CommandContext cmdContext, VisualisationUtility modulePlugin) {
		
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(this.referenceName, this.featureName));
		String referenceNucleotides = 
				featureLoc.getReferenceSequence().getSequence().getSequenceObject().getNucleotides(cmdContext);

		Translator translator = new CommandContextTranslator(cmdContext);

		
		// initial alignment uspace includes just the reference sequence.
		int refLength = referenceNucleotides.length();
		AllColumnsAlignment<String> allColumnsAlmt = new AllColumnsAlignment<String>("reference", refLength);

		allColumnsAlmt.addRow("query", "reference", queryToRefSegments, queryNucleotides.length());

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

		VisualisationAnnotationRow refNtContentRow = new VisualisationAnnotationRow();
		refNtContentRow.annotationType = "refNtContent";
		VisualisationAnnotationRow refNtIndexRow = new VisualisationAnnotationRow();
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
			refNtIndexRow.annotations.add(refStartAnnotation);
			RefNtIndexAnnotation refEndAnnotation = new RefNtIndexAnnotation();
			refEndAnnotation.displayNtPos = seg.getRefEnd()-displayNtOffset;
			refEndAnnotation.ntIndex = seg.getQueryEnd();
			refNtIndexRow.annotations.add(refEndAnnotation);
		});


		int uFeatureStart = ReferenceSegment.minRefStart(refToUFeatureLocSegs);
		int uFeatureEnd = ReferenceSegment.maxRefEnd(refToUFeatureLocSegs);
		
		List<QueryAlignedSegment> queryToUSegs = allColumnsAlmt.getSegments("query");

		List<QueryAlignedSegment> queryToUFeatureLocSegs = 
				ReferenceSegment.intersection(queryToUSegs, Arrays.asList(new ReferenceSegment(uFeatureStart, uFeatureEnd)), 
						ReferenceSegment.cloneLeftSegMerger());

		VisualisationAnnotationRow queryNtContentRow = new VisualisationAnnotationRow();
		queryNtContentRow.annotationType = "queryNtContent";
		VisualisationAnnotationRow queryNtIndexRow = new VisualisationAnnotationRow();
		queryNtIndexRow.annotationType = "queryNtIndex";
		queryToUFeatureLocSegs.forEach(seg -> {
			QueryNtContentAnnotation queryNtContent = new QueryNtContentAnnotation();
			queryNtContent.displayNtPos = seg.getRefStart()-displayNtOffset;
			queryNtContent.ntContent = FastaUtils.subSequence(queryNucleotides, seg.getQueryStart(), seg.getQueryEnd()).toString();
			queryNtContentRow.annotations.add(queryNtContent);
			QueryNtIndexAnnotation queryStartAnnotation = new QueryNtIndexAnnotation();
			queryStartAnnotation.displayNtPos = seg.getRefStart()-displayNtOffset;
			queryStartAnnotation.ntIndex = seg.getQueryStart();
			queryNtIndexRow.annotations.add(queryStartAnnotation);
			QueryNtIndexAnnotation queryEndAnnotation = new QueryNtIndexAnnotation();
			queryEndAnnotation.displayNtPos = seg.getRefEnd()-displayNtOffset;
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
			}
			queryAaRow = new VisualisationAnnotationRow();
			queryAaRow.annotationType = "queryAa";

			List<LabeledQueryAminoAcid> queryLqaas = 
					featureLoc.translateQueryNucleotides(cmdContext, translator, queryToRefSegments, 
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
				queryAaRow.annotations.add(queryAaContentAnnotation);
			}
		
		}

		FeatureVisualisation featureVisualisation = new FeatureVisualisation();
		featureVisualisation.referenceName = featureLoc.getReferenceSequence().getName();
		featureVisualisation.referenceDisplayName = featureLoc.getReferenceSequence().getRenderedName();
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
		
		return new PojoCommandResult<FeatureVisualisation>(featureVisualisation);
		
	}
	
}
