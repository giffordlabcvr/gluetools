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
		return new ListResult(cmdContext, FeatureLocation.class, featureLocs);

	}

}
