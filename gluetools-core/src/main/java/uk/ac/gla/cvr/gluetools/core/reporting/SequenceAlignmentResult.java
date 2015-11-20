package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceRealisedFeatureTreeResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sam2ConsensusMinorityVariantFilter;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public class SequenceAlignmentResult {
	private String alignmentName;
	private String referenceName;
	private Integer referenceLength;
	private List<QueryAlignedSegment> seqToRefAlignedSegments;
	private double seqToRefQueryCoverage;
	private double seqToRefReferenceCoverage;
	private Map<String, SequenceFeatureResult> featureToSequenceFeatureResult = new LinkedHashMap<String, SequenceFeatureResult>();
	
	public SequenceAlignmentResult(String alignmentName, String referenceName) {
		super();
		this.alignmentName = alignmentName;
		this.referenceName = referenceName;
	}

	public void toDocument(ObjectBuilder seqAlmtAnalysisObj) {
		seqAlmtAnalysisObj.set("alignmentName", alignmentName);
		seqAlmtAnalysisObj.set("referenceName", referenceName);
		seqAlmtAnalysisObj.set("referenceLength", referenceLength);
		seqAlmtAnalysisObj.set("seqToRefQueryCoverage", seqToRefQueryCoverage);
		seqAlmtAnalysisObj.set("seqToRefReferenceCoverage", seqToRefReferenceCoverage);
		ArrayBuilder seqToRefAlignedSegArray = seqAlmtAnalysisObj.setArray("seqToRefAlignedSegment");
		for(QueryAlignedSegment seqToRefAlignedSegment: seqToRefAlignedSegments) {
			seqToRefAlignedSegment.toDocument(seqToRefAlignedSegArray.addObject());
		}
		ArrayBuilder seqFeatureResultArray = seqAlmtAnalysisObj.setArray("sequenceFeatureResult");
		for(SequenceFeatureResult seqFeatureResult: featureToSequenceFeatureResult.values()) {
			seqFeatureResult.toDocument(seqFeatureResultArray.addObject());
		}
	}

	public void generateSequenceAlignmentFeatureResults(CommandContext cmdContext, 
			Map<String, AlignmentResult> almtNameToAlmtResult, SequenceResult seqResult, 
			Sam2ConsensusMinorityVariantFilter s2cMinorityVariantFilter, Set<String> featureRestrictions, Set<String> referenceRestrictions, 
			Set<String> variationRestrictions) {
		AlignmentResult alignmentResult = almtNameToAlmtResult.get(alignmentName);
		if(referenceRestrictions == null || referenceRestrictions.contains(alignmentResult.getReferenceName())) {
			ReferenceRealisedFeatureTreeResult rootResult = alignmentResult.getReferenceFeatureTreeResult();
			for(ReferenceRealisedFeatureTreeResult rootChildTree: rootResult.getChildTrees()) {
				generateFeatureResult(cmdContext, rootChildTree, seqResult.getSeqObj(), s2cMinorityVariantFilter, featureRestrictions, variationRestrictions);
			}
		}
	}

	private void generateFeatureResult(CommandContext cmdContext, ReferenceRealisedFeatureTreeResult featureTreeResult, 
			AbstractSequenceObject querySeqObj, Sam2ConsensusMinorityVariantFilter s2cMinorityVariantFilter, 
			Set<String> featureRestrictions, Set<String> variationRestrictions) {
		String featureName = featureTreeResult.getFeatureName();
		if(!featureTreeResult.isInformational() && ( featureRestrictions == null || featureRestrictions.contains(featureName))) {
			SequenceFeatureResult seqFeatureResult = new SequenceFeatureResult(featureTreeResult);
			seqFeatureResult.init(cmdContext, querySeqObj, seqToRefAlignedSegments, featureToSequenceFeatureResult, s2cMinorityVariantFilter, variationRestrictions);
			featureToSequenceFeatureResult.put(featureTreeResult.getFeatureName(), seqFeatureResult);
		}
		featureTreeResult.getChildTrees().forEach(childFeatureTreeResult -> 
			generateFeatureResult(cmdContext, childFeatureTreeResult, querySeqObj, s2cMinorityVariantFilter, featureRestrictions, variationRestrictions));
	}

	public String getAlignmentName() {
		return alignmentName;
	}

	public List<QueryAlignedSegment> getSeqToRefAlignedSegments() {
		return seqToRefAlignedSegments;
	}

	public void setSeqToRefAlignedSegments(
			List<QueryAlignedSegment> seqToRefAlignedSegments) {
		this.seqToRefAlignedSegments = seqToRefAlignedSegments;
	}

	public void setSeqToRefQueryCoverage(double seqToRefQueryCoverage) {
		this.seqToRefQueryCoverage = seqToRefQueryCoverage;
	}

	public void setSeqToRefReferenceCoverage(double seqToRefReferenceCoverage) {
		this.seqToRefReferenceCoverage = seqToRefReferenceCoverage;
	}

	public Integer getReferenceLength() {
		return referenceLength;
	}

	public void setReferenceLength(Integer referenceLength) {
		this.referenceLength = referenceLength;
	}
	
	public SequenceFeatureResult getSequenceFeatureResult(String featureName) {
		return featureToSequenceFeatureResult.get(featureName);
	}

	public String getReferenceName() {
		return referenceName;
	}
	
	
}