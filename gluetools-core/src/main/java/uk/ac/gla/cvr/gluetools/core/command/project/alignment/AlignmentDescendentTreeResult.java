package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

public class AlignmentDescendentTreeResult extends CommandResult {
	
	public AlignmentDescendentTreeResult(Alignment alignment) {
		super("alignmentDescendentTreeResult");
		addNode(getDocumentBuilder(), alignment);
	}

	private void addNode(ObjectBuilder objectBuilder, Alignment alignment) {
		objectBuilder.set("alignmentName", alignment.getName());
		objectBuilder.set("alignmentDisplayName", alignment.getDisplayName());
		ArrayBuilder childrenArrayBuilder = objectBuilder.setArray("childAlignment");
		alignment.getChildren().forEach(childAlmt -> {
			addNode(childrenArrayBuilder.addObject(), childAlmt);
		});
	}

}
