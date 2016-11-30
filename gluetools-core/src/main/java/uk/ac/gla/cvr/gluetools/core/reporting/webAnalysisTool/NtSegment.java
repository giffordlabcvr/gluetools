package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

public abstract class NtSegment {

	@PojoDocumentField
	public String nts;

	@PojoDocumentField
	public Integer startUIndex;

	@PojoDocumentField
	public Integer endUIndex;

	@PojoDocumentField
	public Integer startSeqIndex;

	@PojoDocumentField
	public Integer endSeqIndex;

	
}
