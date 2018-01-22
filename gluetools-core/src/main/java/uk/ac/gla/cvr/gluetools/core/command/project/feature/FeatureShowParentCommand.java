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

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;

@CommandClass( 
		commandWords={"show", "parent"},
		docoptUsages={""},
		description="Show the parent of this feature"
	) 
public class FeatureShowParentCommand extends FeatureModeCommand<FeatureShowParentCommand.FeatureShowParentResult> {

	@Override
	public FeatureShowParentResult execute(CommandContext cmdContext) {
		Feature feature = lookupFeature(cmdContext);
		String parentName = null;
		Feature parent = feature.getParent();
		if(parent != null) {
			parentName = parent.getName();
		}
		return new FeatureShowParentResult(parentName);
	}
	
	public class FeatureShowParentResult extends MapResult {

		public FeatureShowParentResult(String parentName) {
			super("featureShowParentResult", mapBuilder().put(Feature.PARENT_NAME_PATH, parentName));
		}


	}


}
