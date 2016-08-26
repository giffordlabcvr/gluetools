package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class VariationMatch {

	@PojoResultField
	public String variationName;

	@PojoResultField
	public String variationRenderedName;
	
	@PojoResultField
	public Integer minStartUIndex;

	@PojoResultField
	public Integer maxEndUIndex;

	
	@PojoResultField
	public List<VariationMatchLocation> locations = new ArrayList<VariationMatchLocation>();
	
	// display hint, to prevent overlapping variation hits, they are shown on separate "tracks". 
	// track number starts from 0.
	// non-null only if a "present" match
	@PojoResultField
	public Integer track;

}
