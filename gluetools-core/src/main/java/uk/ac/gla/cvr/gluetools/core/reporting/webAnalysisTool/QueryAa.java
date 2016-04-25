package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class QueryAa extends Aa {

	@PojoResultField
	public List<String> referenceDiffs = new ArrayList<String>();
	
}
