package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class QuerySequenceFeatureAnalysis extends SequenceFeatureAnalysis<QueryAa, QueryNtSegment> {

	@PojoResultField
	public List<VariationMatchGroup> variationMatchGroupPresent = null;

	@PojoResultField
	public List<VariationMatchGroup> variationMatchGroupAbsent = null;

}
