package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider.FeatureProviderException.Code;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public abstract class AlignmentFeatureProvider extends FeatureProvider {

	public static final String GLUE_FEATURE_NAME = "glueFeatureName";
	public static final String MIN_COVERAGE_PCT = "minCoveragePct";
	public static final String MAX_CODING_X_PCT = "maxCodingXPct";
	public static final String FEATURE_KEY = "featureKey";
	public static final String QUALIFIER = "qualifier";
	public static final String SPAN_INSERTIONS = "spanInsertions";
	
	private String glueFeatureName;
	// if the alignment member does not cover at least this percentage of the 
	// named feature on the reference, no GenBank feature is generated.
	private Double minCoveragePct;

	// maximum number of Xs in the translation of a coding feature.
	// if this is exceeded, no GenBank feature is generated
	private Double maxCodingXPct;
	
	// default true: if true, insertions in the alignment member relative to the reference will be spanned.
	private Boolean spanInsertions;

	
	private String featureKey;
	private List<QualifierKeyValueTemplate> qualifierKeyValueTemplates;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.glueFeatureName = PluginUtils.configureStringProperty(configElem, GLUE_FEATURE_NAME, true);
		this.minCoveragePct = Optional.ofNullable(PluginUtils
				.configureDoubleProperty(configElem, MIN_COVERAGE_PCT, 0.0, false, 100.0, true, false))
				.orElse(10.0);
		this.maxCodingXPct = Optional.ofNullable(PluginUtils
				.configureDoubleProperty(configElem, MAX_CODING_X_PCT, 0.0, false, 100.0, true, false))
				.orElse(null);
		this.featureKey = PluginUtils.configureStringProperty(configElem, FEATURE_KEY, true);
		this.qualifierKeyValueTemplates = PluginFactory.createPlugins(pluginConfigContext, QualifierKeyValueTemplate.class, 
				PluginUtils.findConfigElements(configElem, QUALIFIER));
		this.spanInsertions = Optional.ofNullable(PluginUtils
				.configureBooleanProperty(configElem, SPAN_INSERTIONS, false)).orElse(true);

	}

	protected Double getMinCoveragePct() {
		return minCoveragePct;
	}

	protected Double getMaxCodingXPct() {
		return maxCodingXPct;
	}

	protected String getGlueFeatureName() {
		return glueFeatureName;
	}

	protected String getFeatureKey() {
		return featureKey;
	}

	protected Boolean getSpanInsertions() {
		return spanInsertions;
	}

	private Map<String, String> generateQualifierKeyValuesFromFeatureLocation(
			CommandContext cmdContext, String sequenceID, FeatureLocation featureLocation) {
		Map<String, String> qualifierKeyValues = new LinkedHashMap<String, String>();
		qualifierKeyValueTemplates.forEach(qkvt -> 
		{
			if(qkvt.includeForSequenceID(sequenceID)) {
				qualifierKeyValues.put(qkvt.getKey(), qkvt.generateValueFromFeatureLocation(cmdContext, featureLocation));
			}
		});
		return qualifierKeyValues;
	}

	protected GbFeatureSpecification generateGbFeatureSpecification(CommandContext cmdContext, Sequence sequence,
			ReferenceSequence constrainingReference, List<QueryAlignedSegment> allMemberToRefSegments, boolean featureLocationMayBeAbsent) {
				Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(getGlueFeatureName()));
				FeatureLocation featureLocation = 
						GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
								FeatureLocation.pkMap(constrainingReference.getName(), feature.getName()), false);
			
				if(featureLocation == null) {
					if(featureLocationMayBeAbsent) {
						GlueLogger.log(Level.FINEST, "No GenBank feature generated for sequence "+
								sequence.getSource().getName()+"/"+sequence.getSequenceID()+", no feature location "+
								feature.getName()+" found on reference sequence "+constrainingReference.getName());
					} else {
						throw new FeatureProviderException(Code.FEATURE_LOCATION_NOT_FOUND_ON_REFERENCE, 
								feature.getName(), constrainingReference.getName());
					}
				}
				List<ReferenceSegment> fLocSegments = featureLocation.segmentsAsReferenceSegments();
				int referenceFLocNts = IReferenceSegment.totalReferenceLength(fLocSegments);
				if(referenceFLocNts == 0) {
					if(featureLocationMayBeAbsent) {
						GlueLogger.log(Level.FINEST, "No GenBank feature generated for sequence "+
								sequence.getSource().getName()+"/"+sequence.getSequenceID()+" for feature location "+
								featureLocation.pkMap()+"; it was empty on the reference sequence");
						return null;
					} else {
						throw new FeatureProviderException(Code.FEATURE_LOCATION_EMPTY_ON_REFERENCE, 
								feature.getName(), constrainingReference.getName());
					}
				}
				
			
				List<QueryAlignedSegment> memberFeatureSegments = ReferenceSegment.intersection(allMemberToRefSegments, fLocSegments, ReferenceSegment.cloneLeftSegMerger());
				
				int memberFLocNts = IReferenceSegment.totalReferenceLength(memberFeatureSegments);
				
				double memberCoveragePct = 100.0 * ((double) memberFLocNts / (double) referenceFLocNts);
				
				Double minCoveragePct = getMinCoveragePct();
				if(memberCoveragePct < minCoveragePct) {
					GlueLogger.log(Level.FINEST, "No GenBank feature generated for sequence "+
							sequence.getSource().getName()+"/"+sequence.getSequenceID()+" because feature location "+
							featureLocation.pkMap()+": coverage percent "+
							memberCoveragePct+" was less than the minimum "+minCoveragePct);
					return null;
				}
				
				if(feature.codesAminoAcids()) {
					// delete regions at the start and end of the feature which translate to X.
					Translator translator = new CommandContextTranslator(cmdContext);
					List<LabeledQueryAminoAcid> lqaas = featureLocation.translateQueryNucleotides(cmdContext, translator, memberFeatureSegments, sequence.getSequenceObject());
					
					List<ReferenceSegment> xRegionsToDelete = new ArrayList<ReferenceSegment>();
					for(int i = 0; i < lqaas.size(); i++) {
						LabeledQueryAminoAcid lqaa = lqaas.get(i);
						if(lqaa.getLabeledAminoAcid().getTranslationInfo().getSingleCharTranslation() == 'X') {
							xRegionsToDelete.addAll(lqaa.getLabeledAminoAcid().getLabeledCodon().getLcRefSegments());
						} else {
							break;
						}
					}
					for(int i = lqaas.size()-1; i > 0 ; i--) {
						LabeledQueryAminoAcid lqaa = lqaas.get(i);
						if(lqaa.getLabeledAminoAcid().getTranslationInfo().getSingleCharTranslation() == 'X') {
							xRegionsToDelete.addAll(lqaa.getLabeledAminoAcid().getLabeledCodon().getLcRefSegments());
						} else {
							break;
						}
					}
					ReferenceSegment.sortByRefStart(xRegionsToDelete);
					memberFeatureSegments = ReferenceSegment.subtract(memberFeatureSegments, xRegionsToDelete);
				}
				
				Double maxCodingXPct = getMaxCodingXPct();
				if(maxCodingXPct != null && feature.codesAminoAcids()) {
					Translator translator = new CommandContextTranslator(cmdContext);
					int totalAminoAcids = 0;
					int numXs = 0;
					for(int i = 0; i < fLocSegments.size(); i++) {
						ReferenceSegment fLocSegment = fLocSegments.get(i);
						List<QueryAlignedSegment> memberGbFeatureSegs = getMemberGbFeatureSegs(cmdContext, featureLocation, sequence, memberFeatureSegments, fLocSegment);
						List<LabeledQueryAminoAcid> lqaas = featureLocation.translateQueryNucleotides(cmdContext, translator, memberGbFeatureSegs, sequence.getSequenceObject());
						totalAminoAcids += lqaas.size();
						for(LabeledQueryAminoAcid lqaa: lqaas) {
							if(lqaa.getLabeledAminoAcid().getTranslationInfo().getSingleCharTranslation() == 'X') {
								numXs++;
							}
						}
					}
					double codingXPct = ( numXs / (double) totalAminoAcids ) * 100.0;
					if( codingXPct > maxCodingXPct ) {
						GlueLogger.log(Level.FINEST, "No GenBank feature generated for sequence "+
								sequence.getSource().getName()+"/"+sequence.getSequenceID()+" because the proportion of Xs in the translation of "+
								featureLocation.pkMap()+" ("+numXs+"/"+totalAminoAcids+") "+
								"was greater than the maximum "+maxCodingXPct);
						return null;
					}
				}

				
				String featureKey = getFeatureKey();
				Map<String, String> qualifierKeyValues = 
						generateQualifierKeyValuesFromFeatureLocation(cmdContext, sequence.getSequenceID(), featureLocation); 
				List<GbFeatureInterval> gbFeatureIntervals = new ArrayList<GbFeatureInterval>();
				
				for(int i = 0; i < fLocSegments.size(); i++) {
					ReferenceSegment fLocSegment = fLocSegments.get(i);
					List<QueryAlignedSegment> memberGbFeatureSegs = getMemberGbFeatureSegs(cmdContext, featureLocation, sequence, memberFeatureSegments, fLocSegment);
					
					for(int j = 0 ; j < memberGbFeatureSegs.size(); j++) {
						QueryAlignedSegment memberGbFeatureSeg = memberGbFeatureSegs.get(j);
						int startNt = memberGbFeatureSeg.getQueryStart();
						boolean incompleteStart = false;
						int endNt = memberGbFeatureSeg.getQueryEnd();
						boolean incompleteEnd = false; 
						int refStartNt = memberGbFeatureSeg.getRefStart();
						
						if(i == 0 && j == 0 && 
								memberGbFeatureSeg.getRefStart() > fLocSegment.getRefStart()) {
							incompleteStart = true;
						}
						if(i == fLocSegments.size() - 1 && j == memberGbFeatureSegs.size() - 1 && 
								memberGbFeatureSeg.getRefEnd() < fLocSegment.getRefEnd()) {
							incompleteEnd = true;
						}
						gbFeatureIntervals.add(new GbFeatureInterval(startNt, refStartNt, incompleteStart, endNt, incompleteEnd));
					}
					if(gbFeatureIntervals.size() == 0) {
						throw new FeatureProviderException(Code.NO_INTERVALS_GENERATED, sequence.getSource().getName()+"/"+sequence.getSequenceID(), featureKey);
					}
					// for incomplete starts of coding features, correct the reading frame as necessary.
					if(featureLocation.getFeature().codesAminoAcids() && featureKey.equals("CDS")) {
						GbFeatureInterval firstInterval = gbFeatureIntervals.get(0);
						
						if(firstInterval.isIncompleteStart()) {
							Integer refStartNt = firstInterval.getRefStartNt();
							LabeledCodon labeledCodonAtStart = featureLocation.getStartRefNtToLabeledCodon(cmdContext).get(refStartNt);
							
							// location where reading frame starts.
							if(labeledCodonAtStart == null) {
								LabeledCodon labeledCodonAtEnd = featureLocation.getEndRefNtToLabeledCodon(cmdContext).get(refStartNt);
								if(labeledCodonAtEnd != null) {
									qualifierKeyValues.put("codon_start", "2");
								} else {
									qualifierKeyValues.put("codon_start", "3");
								}
							}
						}
					}
			
				}
			
				return new GbFeatureSpecification(gbFeatureIntervals, featureKey, qualifierKeyValues);
			}

	private List<QueryAlignedSegment> getMemberGbFeatureSegs(CommandContext cmdContext, FeatureLocation featureLocation, Sequence sequence, List<QueryAlignedSegment> allMemberToRefSegments,
			ReferenceSegment fLocSegment) {
		String nts = sequence.getSequenceObject().getNucleotides(cmdContext);
		List<QueryAlignedSegment> memberFLocSegments = 
				ReferenceSegment.intersection(allMemberToRefSegments, Arrays.asList(fLocSegment), ReferenceSegment.cloneLeftSegMerger());
		if(getSpanInsertions()) {
			memberFLocSegments = Arrays.asList(new QueryAlignedSegment(
					ReferenceSegment.minRefStart(memberFLocSegments), 
					ReferenceSegment.maxRefEnd(memberFLocSegments), 
					QueryAlignedSegment.minQueryStart(memberFLocSegments), 
					QueryAlignedSegment.maxQueryEnd(memberFLocSegments)));
		}
		// trim segment end regions that are Ns.
		List<QueryAlignedSegment> resultSegs = new ArrayList<QueryAlignedSegment>();
		for(QueryAlignedSegment memberFLocSeg: memberFLocSegments) {
			boolean retain = true;
			while(FastaUtils.nt(nts, memberFLocSeg.getQueryStart()) == 'N') {
				if(memberFLocSeg.getCurrentLength() == 1) {
					retain = false;
					break;
				}
				memberFLocSeg.truncateLeft(1);
			}
			if(retain) {
				while(FastaUtils.nt(nts, memberFLocSeg.getQueryEnd()) == 'N') {
					if(memberFLocSeg.getCurrentLength() == 1) {
						retain = false;
						break;
					}
					memberFLocSeg.truncateRight(1);
				}
			}
			if(retain) {
				resultSegs.add(memberFLocSeg);
			}
		}
		return resultSegs;
	}
	
}
