package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class VariationMatch {

	@PojoResultField
	public String variationName;

	@PojoResultField
	public String variationRenderedName;
	
	@PojoResultField
	public Integer startUIndex;

	@PojoResultField
	public Integer endUIndex;

	// display hint, to prevent overlapping variation hits, they are shown on separate "tracks". 
	// track number starts from 0.
	@PojoResultField
	public Integer track;

}
