package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class VariationCategoryResult {

	@PojoResultField
	public String name;
	
	@PojoResultField
	public String displayName;
	
	@PojoResultField
	public Boolean reportAbsence;
	
}
