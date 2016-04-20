package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class NtAlignedSegment {

	@PojoResultField
	public String nucleotides;
	
	@PojoResultField
	public Integer startSeqIndex;

	@PojoResultField
	public Integer endSeqIndex;

	@PojoResultField
	public Integer startUIndex;

	@PojoResultField
	public Integer endUIndex;

	
}
