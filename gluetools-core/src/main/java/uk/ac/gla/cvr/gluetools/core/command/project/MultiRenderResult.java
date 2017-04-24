package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;

public class MultiRenderResult extends CommandResult {

	public MultiRenderResult(List<CommandDocument> resultCmdDocs) {
		super("multiRenderResult");
		CommandArray resultDocsArray = getCommandDocument().setArray("resultDocument");
		for(CommandDocument resultCmdDoc: resultCmdDocs) {
			CommandObject containerObj = resultDocsArray.addObject();
			containerObj.setObject(resultCmdDoc.getRootName(), resultCmdDoc);
		}
	}

}
