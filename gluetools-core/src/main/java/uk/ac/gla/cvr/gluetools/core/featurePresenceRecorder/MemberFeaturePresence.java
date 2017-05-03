package uk.ac.gla.cvr.gluetools.core.featurePresenceRecorder;

import java.util.Map;

public class MemberFeaturePresence {

	private Map<String,String> memberPkMap;
	private Map<String,String> featureLocationPkMap;
	private Double referenceNtCoverage;
	
	public MemberFeaturePresence(Map<String, String> memberPkMap,
			Map<String, String> featureLocationPkMap, Double referenceNtCoverage) {
		super();
		this.memberPkMap = memberPkMap;
		this.featureLocationPkMap = featureLocationPkMap;
		this.referenceNtCoverage = referenceNtCoverage;
	}

	public Map<String, String> getMemberPkMap() {
		return memberPkMap;
	}

	public Map<String, String> getFeatureLocationPkMap() {
		return featureLocationPkMap;
	}

	public Double getReferenceNtCoverage() {
		return referenceNtCoverage;
	}
}
