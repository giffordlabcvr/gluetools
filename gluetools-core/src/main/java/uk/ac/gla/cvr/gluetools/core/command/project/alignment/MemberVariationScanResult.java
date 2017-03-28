package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResultRow;

public class MemberVariationScanResult {

	private Map<String,String> memberPkMap;
	private VariationScanResultRow variationScanResultRow;
	
	public MemberVariationScanResult(AlignmentMember alignmentMember, VariationScanResultRow variationScanResultRow) {
		super();
		this.memberPkMap = alignmentMember.pkMap();
		this.variationScanResultRow = variationScanResultRow;
	}

	public Map<String,String> getMemberPkMap() {
		return memberPkMap;
	}

	public VariationScanResultRow getVariationScanResultRow() {
		return variationScanResultRow;
	}
	
}
