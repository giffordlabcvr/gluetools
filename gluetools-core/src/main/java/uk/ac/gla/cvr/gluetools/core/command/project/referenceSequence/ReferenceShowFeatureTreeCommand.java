package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag.Type;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"show", "feature", "tree"},
		docoptUsages={"[-i]"},
		docoptOptions={"-i, --includeHidden  include features with the HIDDEN metatag"},
		description="Show a tree of features for which the reference has a location"
)
public class ReferenceShowFeatureTreeCommand extends ReferenceSequenceModeCommand<ReferenceShowFeatureTreeCommand.ReferenceShowFeatureTreeResult> {


	public static final String INCLUDE_HIDDEN = "includeHidden";

	private boolean includeHidden;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		includeHidden = PluginUtils.configureBooleanProperty(configElem, INCLUDE_HIDDEN, true);
	}


	@Override
	public ReferenceShowFeatureTreeResult execute(CommandContext cmdContext) {
		ReferenceSequence refSeq = lookupRefSeq(cmdContext);
		ReferenceShowFeatureTreeResult result = new ReferenceShowFeatureTreeResult(includeHidden);
		List<FeatureLocation> featureLocations = new ArrayList<FeatureLocation>(refSeq.getFeatureLocations());
		// sort feature locations by start NT index, adding those which have segments defined before those that don't.
		// if two feature locations start at the same NT index, add first the feature location higher up the feature tree, 
		// breaking ties by feature name.
		// if neither feature location has a segment defined, sort by feature name.
		Collections.sort(featureLocations, new Comparator<FeatureLocation>() {
			@Override
			public int compare(FeatureLocation o1, FeatureLocation o2) {
				List<FeatureSegment> o1Segs = o1.getSegments();
				List<FeatureSegment> o2Segs = o2.getSegments();
				if(o1Segs.size() > 0) {
					if(o2Segs.size() > 0) {
						int refStartComparison = Integer.compare(o1Segs.get(0).getRefStart(), o2Segs.get(0).getRefStart());
						if(refStartComparison != 0) {
							return refStartComparison;
						}
						int featureDepthComparison = Integer.compare(o1.getFeature().getDepthInTree(), o2.getFeature().getDepthInTree());
						if(featureDepthComparison != 0) {
							return featureDepthComparison;
						}
						return o1.getFeature().getName().compareTo(o2.getFeature().getName());
					} else {
						return -1;
					}
				} else {
					if(o2Segs.size() > 0) {
						return 1;
					} else {
						return o1.getFeature().getName().compareTo(o2.getFeature().getName());
					} 
				}
			}
		});
		for(FeatureLocation featureLocation: featureLocations) {
			result.addFeatureLocation(featureLocation);
		}
		return result;
	}

	
	public static class ReferenceShowFeatureTreeResult extends CommandResult {

		private boolean includeHidden;
		
		private Map<String, ObjectBuilder> featureNameToResultObject = 
				new LinkedHashMap<String, ObjectBuilder>();
		private Map<String, Integer> featureNameToCodon1Start = 
				new LinkedHashMap<String, Integer>();
		
		protected ReferenceShowFeatureTreeResult(boolean includeHidden) {
			super("referenceShowFeatureTreeResult");
			this.includeHidden = includeHidden;
		}
		
		public ObjectBuilder addFeature(Feature feature) {
			ObjectBuilder objectBuilder = featureNameToResultObject.get(feature.getName());
			if(objectBuilder != null) {
				return objectBuilder;
			}
			Feature parentFeature = feature.getParent();
			ObjectBuilder parentObjectBuilder = null;
			if(parentFeature == null) {
				parentObjectBuilder = getDocumentBuilder();
			} else {
				parentObjectBuilder = addFeature(parentFeature);
			}
			
			Set<FeatureMetatag.Type> metatagTypes = feature.getMetatagTypes();
			if(!includeHidden && metatagTypes.contains(FeatureMetatag.Type.HIDDEN)) {
				featureNameToResultObject.put(feature.getName(), parentObjectBuilder);
				return parentObjectBuilder;
			} else {
				ArrayBuilder featuresArray = parentObjectBuilder.setArray("features");
				objectBuilder = featuresArray.addObject();
				objectBuilder.set("featureName", feature.getName());
				objectBuilder.set("featureDescription", feature.getDescription());
				Feature orfAncestor = feature.getOrfAncestor();
				if(orfAncestor != null) {
					objectBuilder.set("orfAncestorFeature", orfAncestor.getName());
				}
				ArrayBuilder metatagArray = objectBuilder.setArray("featureMetatag");
				metatagTypes.forEach(t -> metatagArray.addString(t.name()));
				featureNameToResultObject.put(feature.getName(), objectBuilder);
				return objectBuilder;
			}
		}

		public void addFeatureLocation(FeatureLocation featureLocation) {
			Feature feature = featureLocation.getFeature();
			ObjectBuilder objectBuilder = addFeature(feature);
			Set<Type> metatags = feature.getMetatagTypes();
			if(includeHidden || !metatags.contains(FeatureMetatag.Type.HIDDEN)) {
				List<FeatureSegment> referenceSegments = featureLocation.getSegments();
				if(!referenceSegments.isEmpty()) {
					if(feature.getOrfAncestor() != null) {
						int codon1Start;
						if(metatags.contains(FeatureMetatag.Type.OWN_CODON_NUMBERING)) {
							codon1Start = referenceSegments.get(0).getRefStart();
						} else {
							// parent feature will have been added first according to ordering above.
							String parentFeatureName = feature.getParent().getName();
							codon1Start = featureNameToCodon1Start.get(parentFeatureName);
						}
						featureNameToCodon1Start.put(feature.getName(), codon1Start);
						objectBuilder.set("codon1Start", codon1Start);
					}
					ArrayBuilder refSegArray = objectBuilder.setArray("referenceSegment");
					referenceSegments.forEach(refSeg -> {
						refSeg.toDocument(refSegArray.addObject());
					});
				}
			}
		}
	}


}
