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
