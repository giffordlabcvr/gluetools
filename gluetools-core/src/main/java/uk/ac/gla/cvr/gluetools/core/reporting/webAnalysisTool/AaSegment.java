package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class AaSegment {

	@PojoResultField
	public String aaTranslation;
	
	@PojoResultField
	public Integer startUIndex;

	@PojoResultField
	public Integer endUIndex;

}
