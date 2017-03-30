package uk.ac.gla.cvr.gluetools.core.datamodel.refSequence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

/**
 * This result object encapsulates all the feature locations of a reference sequence, in a tree, with feature metatags, 
 * variations etc.
 */

public class ReferenceFeatureTreeResult extends CommandResult {

	private CommandObject commandObject = null;
	
	private List<ReferenceSegment> referenceSegments = new ArrayList<ReferenceSegment>();
	private String referenceName;
	
	private ReferenceFeatureTreeResult parentTreeResult;
	
	private Map<String, ReferenceFeatureTreeResult> featureNameToTreeResult = 
			new LinkedHashMap<String, ReferenceFeatureTreeResult>();

	protected ReferenceFeatureTreeResult(String referenceName) {
		super("referenceFeatureTreeResult");
		this.referenceName = referenceName;
	}

	protected ReferenceFeatureTreeResult(String referenceName, ReferenceFeatureTreeResult parentTreeResult, CommandObject commandObject) {
		this(referenceName);
		this.parentTreeResult = parentTreeResult;
		this.commandObject = commandObject;
	}


	private CommandObject getCommandObject() {
		if(commandObject != null) {
			return commandObject;
		}
		return getCommandDocument();
	}
	

	public ReferenceFeatureTreeResult findFeatureTree(String featureName) {
		String thisFeatureName = this.getFeatureName();
		if(thisFeatureName != null && thisFeatureName.equals(featureName)) {
			return this;
		}
		for(ReferenceFeatureTreeResult childTree: getChildTrees()) {
			ReferenceFeatureTreeResult featureTree = childTree.findFeatureTree(featureName);
			if(featureTree != null) {
				return featureTree;
			}
		}
		return null;
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
		CommandArray featuresArray = parentFeatureTreeResult.getCommandObject().getArray("features");
		if(featuresArray == null) {
			featuresArray = parentFeatureTreeResult.getCommandObject().setArray("features");
		}
		CommandObject childCommandObject = featuresArray.addObject();
		featureToCommandObject(feature, childCommandObject);
		featureTreeResult = createChildFeatureTreeResult(parentFeatureTreeResult, childCommandObject);
		parentFeatureTreeResult.featureNameToTreeResult.put(feature.getName(), featureTreeResult);
		return featureTreeResult;
	}

	protected ReferenceFeatureTreeResult createChildFeatureTreeResult(
			ReferenceFeatureTreeResult parentFeatureTreeResult,
			CommandObject childCommandObject) {
		return new ReferenceFeatureTreeResult(referenceName, parentFeatureTreeResult, childCommandObject);
	}

	private void featureToCommandObject(Feature feature, CommandObject commandObject) {
		Set<FeatureMetatag.Type> metatagTypes = feature.getMetatagTypes();
		commandObject.set("referenceName", referenceName);
		commandObject.set("featureName", feature.getName());
		commandObject.set("featureRenderedName", feature.getRenderedName());
		commandObject.set("featureDescription", feature.getDescription());
		CommandArray metatagArray = commandObject.setArray("featureMetatag");
		metatagTypes.forEach(t -> metatagArray.addString(t.name()));
	}

	public ReferenceFeatureTreeResult addFeatureLocation(CommandContext cmdContext, FeatureLocation featureLocation) {
		Feature feature = featureLocation.getFeature();
		ReferenceFeatureTreeResult featureTreeResult = addFeature(feature);
		CommandObject commandObject = featureTreeResult.getCommandObject();
		if(feature.codesAminoAcids()) {
			Integer codon1Start = featureLocation.getCodon1Start(cmdContext);
			commandObject.setInt("codon1Start", codon1Start);
			LabeledCodon firstLabeledCodon = featureLocation.getFirstLabeledCodon(cmdContext);
			LabeledCodon lastLabeledCodon = featureLocation.getLastLabeledCodon(cmdContext);
			commandObject.setString("firstCodon", firstLabeledCodon.getCodonLabel());
			commandObject.setString("lastCodon", lastLabeledCodon.getCodonLabel());
			if(!feature.hasOwnCodonNumbering()) {
				Feature codonNumberingAncestorFeature = featureLocation.getCodonNumberingAncestorLocation(cmdContext).getFeature();
				commandObject.setString("codonNumberingAncestorFeatureName", codonNumberingAncestorFeature.getName());
				commandObject.setString("codonNumberingAncestorFeatureRenderedName", codonNumberingAncestorFeature.getRenderedName());
			}
		}
		List<FeatureSegment> featureLocSegments = featureLocation.getSegments();
		featureTreeResult.referenceSegments.addAll(featureLocSegments.stream()
				.map(featureLocSeg -> new ReferenceSegment(featureLocSeg.getRefStart(), featureLocSeg.getRefEnd()))
				.collect(Collectors.toList()));
		CommandArray refSegArray = commandObject.setArray("referenceSegment");
		featureTreeResult.referenceSegments.forEach(refSeg -> {
			refSeg.toDocument(refSegArray.addObject());
		});
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
		return getCommandObject().getInteger("codon1Start");
	}

	public String getOrfAncestorFeatureName() {
		return getCommandObject().getString("orfAncestorFeature");
	}

	public String getFeatureName() {
		return getCommandObject().getString("featureName");
	}

	public String getReferenceName() {
		return referenceName;
	}

	public Set<String> getFeatureMetatags() {
		CommandArray featureMetatagArray = getCommandObject().getArray("featureMetatag");
		Set<String> metatags = new LinkedHashSet<String>();
		for(int i = 0; i < featureMetatagArray.size(); i++) {
			metatags.add(featureMetatagArray.getString(i));
		}
		return metatags;
	}

	public List<ReferenceSegment> getReferenceSegments() {
		return referenceSegments;
	}

	public boolean isInformational() {
		return getFeatureMetatags().contains(FeatureMetatag.Type.INFORMATIONAL.name());
	}

	public boolean isIncludedInSummary() {
		return getFeatureMetatags().contains(FeatureMetatag.Type.INCLUDE_IN_SUMMARY.name());
	}

}