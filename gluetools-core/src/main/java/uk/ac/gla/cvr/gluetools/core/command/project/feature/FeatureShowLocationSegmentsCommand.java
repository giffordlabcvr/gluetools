package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.feature.FeatureShowLocationSegmentsCommand.FeatureShowLocationSegmentsResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;

@CommandClass( 
		commandWords={"show", "location"},
		docoptUsages={""},
		description="Show locations of the feature on different references"
	) 
public class FeatureShowLocationSegmentsCommand extends FeatureModeCommand<FeatureShowLocationSegmentsResult> {

	
	public static class FeatureShowLocationSegmentsResult extends TableResult {

		public FeatureShowLocationSegmentsResult(List<Map<String, Object>> rowData) {
			super("featureShowLocationSegmentsResult", 
					Arrays.asList("refName", "segments"), rowData);
		}
		
	}

	@Override
	public FeatureShowLocationSegmentsResult execute(CommandContext cmdContext) {
		Feature feature = super.lookupFeature(cmdContext);
		List<Map<String, Object>> rowData =
				feature.getFeatureLocations().stream()
				.map(fLoc -> {
					Map<String, Object> map = new LinkedHashMap<String, Object>();
					map.put("refName", fLoc.getReferenceSequence().getName());
					map.put("segments", 
							String.join(", ", fLoc.getSegments().stream()
							.map(seg -> seg.toString())
							.collect(Collectors.toList())));
					return map;
				})
				.collect(Collectors.toList());
		return new FeatureShowLocationSegmentsResult(rowData);
	}
	
}
