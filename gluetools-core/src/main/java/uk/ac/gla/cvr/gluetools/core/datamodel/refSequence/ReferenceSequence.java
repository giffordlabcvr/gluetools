package uk.ac.gla.cvr.gluetools.core.datamodel.refSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueConfigContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;

@GlueDataClass(defaultListColumns = {_ReferenceSequence.NAME_PROPERTY, ReferenceSequence.SEQ_SOURCE_NAME_PATH, ReferenceSequence.SEQ_ID_PATH})
public class ReferenceSequence extends _ReferenceSequence {

	public static final String SEQ_SOURCE_NAME_PATH = 
			_ReferenceSequence.SEQUENCE_PROPERTY+"."+
					_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY;
	
	public static final String SEQ_ID_PATH = 
			_ReferenceSequence.SEQUENCE_PROPERTY+"."+_Sequence.SEQUENCE_ID_PROPERTY;


	
	public static Map<String, String> pkMap(String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}
	

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}

	@Override
	protected Map<String, String> pkMap() {
		return pkMap(getName());
	}


	public void validate(CommandContext cmdContext) {
		getFeatureLocations().forEach(featureLoc -> featureLoc.validate(cmdContext));

	}


	public ReferenceFeatureTreeResult getFeatureTree(CommandContext cmdContext, Feature limitingFeature, boolean recursive) {
		ReferenceFeatureTreeResult featureTree = new ReferenceFeatureTreeResult();
		buildTree(cmdContext, limitingFeature, recursive, featureTree);
		return featureTree;
	}

	public ReferenceRealisedFeatureTreeResult getRealisedFeatureTree(CommandContext cmdContext, Feature limitingFeature, boolean recursive) {
		ReferenceRealisedFeatureTreeResult featureTree = new ReferenceRealisedFeatureTreeResult();
		buildTree(cmdContext, limitingFeature, recursive, featureTree);
		return featureTree;
	}

	public void buildTree(CommandContext cmdContext,
			Feature limitingFeature, boolean recursive, ReferenceFeatureTreeResult featureTree) {
		List<FeatureLocation> featureLocations = new ArrayList<FeatureLocation>(getFeatureLocations());
		Collections.sort(featureLocations, new FeatureLocationComparator());
		for(FeatureLocation featureLocation: featureLocations) {
			Feature feature = featureLocation.getFeature();
			if(limitingFeature == null || 
					feature.getName().equals(limitingFeature.getName()) ||
					(recursive && feature.isDescendentOf(limitingFeature)) || 
					limitingFeature.isDescendentOf(feature) ) {
				featureTree.addFeatureLocation(cmdContext, featureLocation);
			}
 		}
	}


	// sort feature locations by start NT index, adding those which have segments defined before those that don't.
	// if two feature locations start at the same NT index, add first the feature location higher up the feature tree, 
	// breaking ties by feature name.
	// if neither feature location has a segment defined, sort by feature name.
	private class FeatureLocationComparator implements Comparator<FeatureLocation> {
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
	}

	public void generateGlueConfig(int indent, StringBuffer glueConfigBuf, GlueConfigContext glueConfigContext) {
		if(glueConfigContext.includeVariations()) {
			for(FeatureLocation featureLoc: getFeatureLocations()) {
				StringBuffer featureLocConfigBuf = new StringBuffer();
				featureLoc.generateGlueConfig(indent+INDENT, featureLocConfigBuf, glueConfigContext);
				if(featureLocConfigBuf.length() > 0) {
					indent(glueConfigBuf, indent).append("feature-location ").append(featureLoc.getFeature().getName()).append("\n");
					glueConfigBuf.append(featureLocConfigBuf.toString());
					indent(glueConfigBuf, indent+INDENT).append("exit\n");
				}
			}
			
		}
	}

	
	
}
