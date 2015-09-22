package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceFeatureTreeResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sam2ConsensusMinorityVariantFilter;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public class SequenceAlignmentResult {
	private String alignmentName;
	private String referenceName;
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
			Sam2ConsensusMinorityVariantFilter s2cMinorityVariantFilter) {
		AlignmentResult alignmentResult = almtNameToAlmtResult.get(alignmentName);
		ReferenceFeatureTreeResult rootResult = alignmentResult.getReferenceFeatureTreeResult();
		for(ReferenceFeatureTreeResult rootChildTree: rootResult.getChildTrees().values()) {
			generateFeatureResult(cmdContext, rootChildTree, seqResult.getSeqObj(), s2cMinorityVariantFilter);
		}
	}

	private void generateFeatureResult(CommandContext cmdContext, ReferenceFeatureTreeResult featureTreeResult, 
			AbstractSequenceObject querySeqObj, Sam2ConsensusMinorityVariantFilter s2cMinorityVariantFilter) {
		if(!featureTreeResult.isInformational()) {
			SequenceFeatureResult seqFeatureResult = new SequenceFeatureResult(featureTreeResult);
			seqFeatureResult.init(cmdContext, querySeqObj, seqToRefAlignedSegments, featureToSequenceFeatureResult, s2cMinorityVariantFilter);
			featureToSequenceFeatureResult.put(featureTreeResult.getFeatureName(), seqFeatureResult);
		}
		featureTreeResult.getChildTrees().values().forEach(childFeatureTreeResult -> 
		generateFeatureResult(cmdContext, childFeatureTreeResult, querySeqObj, s2cMinorityVariantFilter));
	}

	public String getAlignmentName() {
		return alignmentName;
	}

	public void setAlignmentName(String alignmentName) {
		this.alignmentName = alignmentName;
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
	
	
	
	
}