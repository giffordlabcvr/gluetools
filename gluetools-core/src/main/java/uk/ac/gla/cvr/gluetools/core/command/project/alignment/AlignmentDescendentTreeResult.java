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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.utils.CayenneUtils;

public class AlignmentDescendentTreeResult extends CommandResult {
	
	private List<Ordering> orderings;
	
	public AlignmentDescendentTreeResult(CommandContext cmdContext, Alignment alignment, String sortProperties) {
		super("alignmentDescendentTreeResult");
		if(sortProperties != null) {
			orderings = CayenneUtils.sortPropertiesToOrderings(sortProperties);
		}
		addNode(cmdContext, getCommandDocument(), alignment);
	}

	private void addNode(CommandContext cmdContext, CommandObject objectBuilder, Alignment alignment) {
		String parentName = alignment.getName();
		objectBuilder.set("alignmentName", parentName);
		objectBuilder.set("alignmentRenderedName", alignment.getRenderedName());
		CommandArray childrenArrayBuilder = objectBuilder.setArray("childAlignment");
		SelectQuery selectQuery = new SelectQuery(Alignment.class, ExpressionFactory.matchExp(Alignment.PARENT_NAME_PATH, parentName));
		if(orderings != null) {
			selectQuery.addOrderings(orderings);
		}
		List<Alignment> childAlmts = GlueDataObject.query(cmdContext, Alignment.class, selectQuery);
		childAlmts.forEach(childAlmt -> {
			addNode(cmdContext, childrenArrayBuilder.addObject(), childAlmt);
		});
	}

}
