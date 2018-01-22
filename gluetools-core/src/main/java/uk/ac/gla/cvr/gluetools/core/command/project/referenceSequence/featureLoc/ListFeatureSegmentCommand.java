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
