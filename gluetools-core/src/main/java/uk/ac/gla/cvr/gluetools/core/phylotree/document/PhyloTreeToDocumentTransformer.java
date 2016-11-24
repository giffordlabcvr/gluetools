package uk.ac.gla.cvr.gluetools.core.phylotree.document;

import java.util.LinkedList;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.document.CommandValue;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloObject;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;


public class PhyloTreeToDocumentTransformer implements PhyloTreeVisitor {

	private CommandDocument commandDocument;
	private LinkedList<CommandValue> commandValueStack = new LinkedList<CommandValue>();
	
	public CommandDocument getDocument() {
		return commandDocument;
	}

	@Override
	public void preVisitTree(PhyloTree phyloTree) {
		commandDocument = new CommandDocument("phlyoTree");
		writeUserData(phyloTree, commandDocument.setObject("userData"));
		commandValueStack.push(commandDocument.setObject("root"));
	}

	@Override
	public void postVisitTree(PhyloTree phyloTree) {
		commandValueStack.pop();
	}

	@Override
	public void preVisitInternal(PhyloInternal phyloInternal) {
		CommandObject internalObj = ((CommandObject) commandValueStack.peek()).setObject("internal");
		writeUserData(phyloInternal, internalObj);
		commandValueStack.push(internalObj);
		commandValueStack.push(internalObj.setArray("branch"));
	}

	@Override
	public void postVisitInternal(PhyloInternal phyloInternal) {
		commandValueStack.pop(); // pop branches array
		commandValueStack.pop(); // pop internal obj
	}

	@Override
	public void visitLeaf(PhyloLeaf phyloLeaf) {
		CommandObject leafObj = ((CommandObject) commandValueStack.peek()).setObject("leaf");
		writeUserData(phyloLeaf, leafObj);
	}

	@Override
	public void preVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
		CommandObject branchObj = ((CommandArray) commandValueStack.peek()).addObject();
		writeUserData(phyloBranch, branchObj);
		commandValueStack.push(branchObj);
	}

	@Override
	public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
		commandValueStack.pop();
	}

	private void writeUserData(PhyloObject<?> phyloObject, CommandObject userDataObj) {
		Map<String, Object> userData = phyloObject.getUserData();
		if(userData != null) {
			userData.forEach((k,v) -> {
				userDataObj.set(k, v);
			});
		}
	}

}
