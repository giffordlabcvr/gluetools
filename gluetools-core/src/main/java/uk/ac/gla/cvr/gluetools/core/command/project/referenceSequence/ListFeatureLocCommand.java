package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;


@CommandClass(
	commandWords={"list", "feature-location"}, 
	docoptUsages={""},
	description="List the feature loctions for the reference") 
public class ListFeatureLocCommand extends ReferenceSequenceModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(FeatureLocation.REF_SEQ_NAME_PATH, getRefSeqName());
		List<FeatureLocation> featureLocs = GlueDataObject.query(cmdContext, FeatureLocation.class, new SelectQuery(FeatureLocation.class, exp));
		Collections.sort(featureLocs, new Comparator<FeatureLocation>(){
			@Override
			public int compare(FeatureLocation fLoc1, FeatureLocation fLoc2) {
				return Feature.compareDisplayOrderKeyLists(fLoc1.getFeature().getDisplayOrderKeyList(), fLoc2.getFeature().getDisplayOrderKeyList());
			}
		});
		return new ListResult(FeatureLocation.class, featureLocs);

	}

}
