package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class CodonLabel {

	@PojoResultField
	public String label;

	@PojoResultField
	public Integer startUIndex;

	@PojoResultField
	public Integer endUIndex;
	
}
