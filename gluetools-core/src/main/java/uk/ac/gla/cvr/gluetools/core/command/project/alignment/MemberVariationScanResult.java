package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

public class MemberVariationScanResult {

	private AlignmentMember alignmentMember;
	private VariationScanResult variationScanResult;
	
	public MemberVariationScanResult(AlignmentMember alignmentMember, VariationScanResult variationScanResult) {
		super();
		this.alignmentMember = alignmentMember;
		this.variationScanResult = variationScanResult;
	}

	public AlignmentMember getAlignmentMember() {
		return alignmentMember;
	}

	public VariationScanResult getVariationScanResult() {
		return variationScanResult;
	}
	
}
