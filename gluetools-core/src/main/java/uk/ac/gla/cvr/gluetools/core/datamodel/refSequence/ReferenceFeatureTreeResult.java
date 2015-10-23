package uk.ac.gla.cvr.gluetools.core.datamodel.refSequence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationDocument;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ArrayReader;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

/**
 * This result object encapsulates all the feature locations of a reference sequence, in a tree, with feature metatags, 
 * variations etc.
 */

public class ReferenceFeatureTreeResult extends CommandResult {

	private ObjectBuilder objectBuilder = null;
	private ObjectReader objectReader = null;
	
	private List<ReferenceSegment> referenceSegments = new ArrayList<ReferenceSegment>();
	private List<VariationDocument> variationDocuments = new ArrayList<VariationDocument>();
	
	private ReferenceFeatureTreeResult parentTreeResult;
	
	private Map<String, ReferenceFeatureTreeResult> featureNameToTreeResult = 
			new LinkedHashMap<String, ReferenceFeatureTreeResult>();

	protected ReferenceFeatureTreeResult() {
		super("referenceFeatureTreeResult");
	}

	protected ReferenceFeatureTreeResult(ReferenceFeatureTreeResult parentTreeResult, ObjectBuilder objectBuilder) {
		this();
		this.parentTreeResult = parentTreeResult;
		this.objectBuilder = objectBuilder;
 	 	this.objectReader = objectBuilder.getObjectReader();
	}


	private ObjectReader getObjectReader() {
		if(objectReader != null) {
			return objectReader;
		}
		return getDocumentReader();
	}
	
	protected ObjectBuilder getObjectBuilder() {
		if(objectBuilder != null) {
			return objectBuilder;
		}
		return getDocumentBuilder();
	}
	
	protected ReferenceFeatureTreeResult addFeature(Feature feature) {
		Feature parentFeature = feature.getParent();
		ReferenceFeatureTreeResult parentFeatureTreeResult = null;
		if(parentFeature == null) {
			parentFeatureTreeResult = this;
		} else {
			parentFeatureTreeResult = addFeature(parentFeature);
		}
		ReferenceFeatureTreeResult featureTreeResult = parentFeatureTreeResult.featureNameToTreeResult.get(feature.getName());
		if(featureTreeResult != null) {
			return featureTreeResult;
		}
		ObjectBuilder objectBuilder = parentFeatureTreeResult.getObjectBuilder().setArray("features").addObject();
		featureToDocument(feature, objectBuilder);
		featureTreeResult = createChildFeatureTreeResult(parentFeatureTreeResult, objectBuilder);
		parentFeatureTreeResult.featureNameToTreeResult.put(feature.getName(), featureTreeResult);
		return featureTreeResult;
	}

	protected ReferenceFeatureTreeResult createChildFeatureTreeResult(
			ReferenceFeatureTreeResult parentFeatureTreeResult,
			ObjectBuilder objectBuilder) {
		return new ReferenceFeatureTreeResult(parentFeatureTreeResult, objectBuilder);
	}

	private void featureToDocument(Feature feature, ObjectBuilder objectBuilder) {
		Set<FeatureMetatag.Type> metatagTypes = feature.getMetatagTypes();
		objectBuilder.set("featureName", feature.getName());
		objectBuilder.set("featureDescription", feature.getDescription());
		Feature orfAncestor = feature.getOrfAncestor();
		if(orfAncestor != null) {
			objectBuilder.set("orfAncestorFeature", orfAncestor.getName());
		}
		ArrayBuilder metatagArray = objectBuilder.setArray("featureMetatag");
		metatagTypes.forEach(t -> metatagArray.addString(t.name()));
	}

	public ReferenceFeatureTreeResult addFeatureLocation(CommandContext cmdContext, FeatureLocation featureLocation) {
		Feature feature = featureLocation.getFeature();
		ReferenceFeatureTreeResult featureTreeResult = addFeature(feature);
		ObjectBuilder objectBuilder = featureTreeResult.getObjectBuilder();
		Integer codon1Start = featureLocation.getCodon1Start(cmdContext);
		if(codon1Start != null) {
			objectBuilder.setInt("codon1Start", codon1Start);
		}
		List<FeatureSegment> featureLocSegments = featureLocation.getSegments();
		featureTreeResult.referenceSegments.addAll(featureLocSegments.stream()
				.map(featureLocSeg -> new ReferenceSegment(featureLocSeg.getRefStart(), featureLocSeg.getRefEnd()))
				.collect(Collectors.toList()));
		ArrayBuilder refSegArray = objectBuilder.setArray("referenceSegment");
		featureTreeResult.referenceSegments.forEach(refSeg -> {
			refSeg.toDocument(refSegArray.addObject());
		});
		ArrayBuilder variationArray = objectBuilder.setArray("variation");
		for(Variation variation: featureLocation.getVariations()) {
			VariationDocument variationDocument = variation.getVariationDocument();
			featureTreeResult.variationDocuments.add(variationDocument);
			variationDocument.toDocument(variationArray.addObject());
		}
		return featureTreeResult;
	}
	
	protected ReferenceFeatureTreeResult findAncestor(String name) {
		if(parentTreeResult == null) {
			return null;
		}
		if(parentTreeResult.getFeatureName().equals(name)) {
			return parentTreeResult;
		}
		return parentTreeResult.findAncestor(name);
	}
	

	public List<? extends ReferenceFeatureTreeResult> getChildTrees() {
		return new ArrayList<ReferenceFeatureTreeResult>(featureNameToTreeResult.values());
	}
	
	public Integer getCodon1Start() {
		return getObjectReader().intValue("codon1Start");
	}

	public String getOrfAncestorFeatureName() {
		return getObjectReader().stringValue("orfAncestorFeature");
	}

	public String getFeatureName() {
		return getObjectReader().stringValue("featureName");
	}

	public Set<String> getFeatureMetatags() {
		ArrayReader featureMetatagArray = getObjectReader().getArray("featureMetatag");
		Set<String> metatags = new LinkedHashSet<String>();
		for(int i = 0; i < featureMetatagArray.size(); i++) {
			metatags.add(featureMetatagArray.stringValue(i));
		}
		return metatags;
	}

	public List<ReferenceSegment> getReferenceSegments() {
		return referenceSegments;
	}

	public List<VariationDocument> getVariationDocuments() {
		return variationDocuments;
	}

	public boolean isInformational() {
		return getFeatureMetatags().contains(FeatureMetatag.Type.INFORMATIONAL.name());
	}

	public boolean isOpenReadingFrame() {
		return getFeatureMetatags().contains(FeatureMetatag.Type.OPEN_READING_FRAME.name());
	}
	
	public boolean isIncludedInSummary() {
		return getFeatureMetatags().contains(FeatureMetatag.Type.INCLUDE_IN_SUMMARY.name());
	}

}