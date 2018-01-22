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
		commandDocument = new CommandDocument("phyloTree");
		writeUserData(phyloTree, commandDocument);
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

	private void writeUserData(PhyloObject<?> phyloObject, CommandObject parentObj) {
		Map<String, Object> userData = phyloObject.getUserData();
		if(userData != null) {
			CommandObject userDataObj = parentObj.setObject("userData");
			userData.forEach((k,v) -> {
				userDataObj.set(k, v);
			});
		}
	}

}
