package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

public abstract class Nt {

	@PojoResultField
	public String nt;

	@PojoResultField
	public Integer uIndex;

	@PojoResultField
	public Boolean segmentBoundary;

	@PojoResultField
	public Integer seqIndex;

}
