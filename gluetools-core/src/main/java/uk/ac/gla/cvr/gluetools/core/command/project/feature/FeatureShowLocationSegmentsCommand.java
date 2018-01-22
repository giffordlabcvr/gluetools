/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
