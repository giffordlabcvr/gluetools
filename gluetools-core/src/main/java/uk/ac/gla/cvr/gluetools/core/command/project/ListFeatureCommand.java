package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;


@CommandClass(
	commandWords={"list", "feature"}, 
	docoptUsages={""},
	description="List genome features") 
public class ListFeatureCommand extends ProjectModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		
		List<Feature> features = GlueDataObject.query(cmdContext, Feature.class, new SelectQuery(Feature.class));
		
		Collections.sort(features, new Comparator<Feature>(){
			@Override
			public int compare(Feature f1, Feature f2) {
				return Feature.compareDisplayOrderKeyLists(f1.getDisplayOrderKeyList(), f2.getDisplayOrderKeyList());
			}
		});
		
		return new ListResult(Feature.class, features);
	}

}
