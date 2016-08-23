package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;


@CommandClass(
	commandWords={"list", "segment"}, 
	docoptUsages={""},
	description="List the reference sequence segments") 
public class ListFeatureSegmentCommand extends FeatureLocModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(FeatureSegment.REF_SEQ_NAME_PATH, getRefSeqName());
		exp = exp.andExp(ExpressionFactory.matchExp(FeatureSegment.FEATURE_NAME_PATH, getFeatureName()));
		
		List<FeatureSegment> segments = GlueDataObject.query(cmdContext, FeatureSegment.class, new SelectQuery(FeatureSegment.class, exp));
		segments = IReferenceSegment.sortByRefStart(segments, ArrayList::new);
		
		return new ListResult(cmdContext, FeatureSegment.class, segments);
	}

}
