package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

@CommandClass(
		commandWords={"show", "feature", "tree"},
		docoptUsages={""},
		docoptOptions={},
		description="Show a tree of features for which the reference has a location"
)
public class ReferenceShowFeatureTreeCommand extends ReferenceSequenceModeCommand<ReferenceShowFeatureTreeCommand.ReferenceShowFeatureTreeResult> {

	@Override
	public ReferenceShowFeatureTreeResult execute(CommandContext cmdContext) {
		ReferenceSequence refSeq = lookupRefSeq(cmdContext);
		ReferenceShowFeatureTreeResult result = new ReferenceShowFeatureTreeResult();
		for(FeatureLocation featureLocation: refSeq.getFeatureLocations()) {
			result.addFeatureLocation(featureLocation);
		}
		return result;
	}

	
	public static class ReferenceShowFeatureTreeResult extends CommandResult {

		private Map<String, ObjectBuilder> featureNameToResultObject = 
				new LinkedHashMap<String, ObjectBuilder>();
		
		protected ReferenceShowFeatureTreeResult() {
			super("referenceShowFeatureTreeResult");
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
			ArrayBuilder featuresArray = parentObjectBuilder.setArray("features");
			objectBuilder = featuresArray.addObject();
			objectBuilder.set("featureName", feature.getName());
			objectBuilder.set("featureDescription", feature.getDescription());
			objectBuilder.set("featureTranscriptionType", feature.getTranscriptionFormat().name());
			featureNameToResultObject.put(feature.getName(), objectBuilder);
			return objectBuilder;
		}

		public void addFeatureLocation(FeatureLocation featureLocation) {
			Feature feature = featureLocation.getFeature();
			ObjectBuilder objectBuilder = addFeature(feature);
			List<FeatureSegment> referenceSegments = featureLocation.getSegments();
			ArrayBuilder refSegArray = objectBuilder.setArray("referenceSegment");
			referenceSegments.forEach(refSeg -> {
				refSeg.toDocument(refSegArray.addObject());
			});
			
		}
		
	}


}
