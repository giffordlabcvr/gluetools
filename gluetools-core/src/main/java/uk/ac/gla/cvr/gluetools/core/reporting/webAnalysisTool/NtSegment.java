package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

public abstract class NtSegment {

	@PojoResultField
	public String nts;

	@PojoResultField
	public Integer startUIndex;

	@PojoResultField
	public Integer endUIndex;

	@PojoResultField
	public Integer startSeqIndex;

	@PojoResultField
	public Integer endSeqIndex;

	
}
