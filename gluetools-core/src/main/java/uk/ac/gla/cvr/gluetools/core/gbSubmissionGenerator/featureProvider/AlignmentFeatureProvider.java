package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import org.w3c.dom.Element;

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
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

public abstract class AlignmentFeatureProvider extends FeatureProvider {

	public static final String GLUE_FEATURE_NAME = "glueFeatureName";
	public static final String MIN_COVERAGE_PCT = "minCoveragePct";
	public static final String FEATURE_KEY = "featureKey";
	public static final String QUALIFIER = "qualifier";
	public static final String SPAN_INSERTIONS = "spanInsertions";
	
	private String glueFeatureName;
	// if the alignment member does not cover at least this percentage of the 
	// named feature on the reference, no GenBank feature is generated.
	private Double minCoveragePct;

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
		this.featureKey = PluginUtils.configureStringProperty(configElem, FEATURE_KEY, true);
		this.qualifierKeyValueTemplates = PluginFactory.createPlugins(pluginConfigContext, QualifierKeyValueTemplate.class, 
				PluginUtils.findConfigElements(configElem, QUALIFIER));
		this.spanInsertions = Optional.ofNullable(PluginUtils
				.configureBooleanProperty(configElem, SPAN_INSERTIONS, false)).orElse(true);

	}

	protected Double getMinCoveragePct() {
		return minCoveragePct;
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
							sequence.getSource().getName()+"/"+sequence.getSequenceID()+" based on feature location "+
							featureLocation.pkMap()+": member coverage percent "+
							memberCoveragePct+" was less than the minimum "+minCoveragePct);
					return null;
				}
				String featureKey = getFeatureKey();
				Map<String, String> qualifierKeyValues = 
						generateQualifierKeyValuesFromFeatureLocation(cmdContext, sequence.getSequenceID(), featureLocation); 
				List<GbFeatureInterval> gbFeatureIntervals = new ArrayList<GbFeatureInterval>();
				
				
				for(int i = 0; i < fLocSegments.size(); i++) {
					ReferenceSegment fLocSegment = fLocSegments.get(i);
					List<QueryAlignedSegment> memberFLocSegments = 
							ReferenceSegment.intersection(allMemberToRefSegments, Arrays.asList(fLocSegment), ReferenceSegment.cloneLeftSegMerger());
					if(getSpanInsertions()) {
						memberFLocSegments = Arrays.asList(new QueryAlignedSegment(
								ReferenceSegment.minRefStart(memberFLocSegments), 
								ReferenceSegment.maxRefEnd(memberFLocSegments), 
								QueryAlignedSegment.minQueryStart(memberFLocSegments), 
								QueryAlignedSegment.maxQueryEnd(memberFLocSegments)));
					}
					for(int j = 0 ; j < memberFLocSegments.size(); j++) {
						QueryAlignedSegment memberFLocSegment = memberFLocSegments.get(j);
						int startNt = memberFLocSegment.getQueryStart();
						boolean incompleteStart = false;
						int endNt = memberFLocSegment.getQueryEnd();
						boolean incompleteEnd = false; 
						int refStartNt = memberFLocSegment.getRefStart();
						
						if(i == 0 && j == 0 && 
								memberFLocSegment.getRefStart() > fLocSegment.getRefStart()) {
							incompleteStart = true;
						}
						if(i == fLocSegments.size() - 1 && j == memberFLocSegments.size() - 1 && 
								memberFLocSegment.getRefEnd() < fLocSegment.getRefEnd()) {
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
							// location on reference where reading frame starts.
							Integer codon1Start = featureLocation.getCodon1Start(cmdContext);
							Integer refStartNt = firstInterval.getRefStartNt();
							if(!TranslationUtils.isAtStartOfCodon(codon1Start, refStartNt)) {
								if(TranslationUtils.isAtEndOfCodon(codon1Start, refStartNt)) {
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
	
}
