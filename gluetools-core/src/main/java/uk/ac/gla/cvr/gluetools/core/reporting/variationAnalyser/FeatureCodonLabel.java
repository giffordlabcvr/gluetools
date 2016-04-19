package uk.ac.gla.cvr.gluetools.core.reporting.variationAnalyser;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class FeatureCodonLabel {

	@PojoResultField
	public String codonLabel;
	
	@PojoResultField
	public Integer startUIndex;

	@PojoResultField
	public Integer endUIndex;

}
