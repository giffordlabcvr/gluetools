package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@CommandClass(
		commandWords={"show", "feature", "tree"},
		docoptUsages={""},
		docoptOptions={},
		description="Show a tree of features for which the reference has a location",
		furtherHelp="Features at the same tree level are listed in order of refStart"
)
public class ReferenceShowFeatureTreeCommand extends ReferenceSequenceModeCommand<ReferenceFeatureTreeResult> {


	@Override
	public ReferenceFeatureTreeResult execute(CommandContext cmdContext) {
		ReferenceSequence refSeq = lookupRefSeq(cmdContext);
		List<FeatureLocation> featureLocations = new ArrayList<FeatureLocation>(refSeq.getFeatureLocations());
		Collections.sort(featureLocations, new FeatureLocationComparator());
		ReferenceFeatureTreeResult result = new ReferenceFeatureTreeResult();
		for(FeatureLocation featureLocation: featureLocations) {
			result.addFeatureLocation(cmdContext, featureLocation);
		}
		return result;
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


}
