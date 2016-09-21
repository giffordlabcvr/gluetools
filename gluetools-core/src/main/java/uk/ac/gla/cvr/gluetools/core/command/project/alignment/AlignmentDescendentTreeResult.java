package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;

public class AlignmentDescendentTreeResult extends CommandResult {
	
	public AlignmentDescendentTreeResult(Alignment alignment) {
		super("alignmentDescendentTreeResult");
		addNode(getCommandDocument(), alignment);
	}

	private void addNode(CommandObject objectBuilder, Alignment alignment) {
		objectBuilder.set("alignmentName", alignment.getName());
		objectBuilder.set("alignmentDisplayName", alignment.getDisplayName());
		CommandArray childrenArrayBuilder = objectBuilder.setArray("childAlignment");
		alignment.getChildren().forEach(childAlmt -> {
			addNode(childrenArrayBuilder.addObject(), childAlmt);
		});
	}

}
