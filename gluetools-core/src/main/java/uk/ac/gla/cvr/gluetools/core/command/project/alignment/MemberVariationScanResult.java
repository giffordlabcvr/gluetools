package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

public class MemberVariationScanResult {

	private Map<String,String> memberPkMap;
	private VariationScanResult variationScanResult;
	
	public MemberVariationScanResult(AlignmentMember alignmentMember, VariationScanResult variationScanResult) {
		super();
		this.memberPkMap = alignmentMember.pkMap();
		this.variationScanResult = variationScanResult;
	}

	public Map<String,String> getMemberPkMap() {
		return memberPkMap;
	}

	public VariationScanResult getVariationScanResult() {
		return variationScanResult;
	}
	
}
