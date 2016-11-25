package uk.ac.gla.cvr.gluetools.core.phylotree.document;

import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.document.CommandArrayItem;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocumentVisitor;
import uk.ac.gla.cvr.gluetools.core.document.CommandFieldValue;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.document.SimpleCommandValue;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloObject;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.document.PhyloDocumentException.Code;

public class DocumentToPhyloTreeTransformer implements CommandDocumentVisitor {

	private PhyloTree phyloTree;
	private PhyloObject<?> currentPhyloObject;
	private Map<String, Object> currentUserData;
	
	public PhyloTree getPhyloTree() {
		return phyloTree;
	}

	@Override
	public void preVisitCommandDocument(CommandDocument commandDocument) {
		this.phyloTree = new PhyloTree();
		this.currentPhyloObject = phyloTree;
	}

	@Override
	public void postVisitCommandDocument(CommandDocument commandDocument) {
		this.currentPhyloObject = null;
	}

	@Override
	public void preVisitCommandObject(String objFieldName, CommandObject commandObject) {
		if(objFieldName.equals("userData")) {
			currentUserData = currentPhyloObject.ensureUserData();
		} else if(objFieldName.equals("root")) {
		} else if(objFieldName.equals("phyloTree")) {
		} else if(objFieldName.equals("branch")) {
		} else if(objFieldName.equals("internal")) {
			PhyloInternal phyloInternal = new PhyloInternal();
			if(currentPhyloObject instanceof PhyloTree) {
				((PhyloTree) currentPhyloObject).setRoot(phyloInternal);
			} else if(currentPhyloObject instanceof PhyloBranch) {
				((PhyloBranch) currentPhyloObject).setSubtree(phyloInternal);
			}
			currentPhyloObject = phyloInternal;
		} else if(objFieldName.equals("leaf")) {
			PhyloLeaf phyloLeaf = new PhyloLeaf();
			if(currentPhyloObject instanceof PhyloTree) {
				((PhyloTree) currentPhyloObject).setRoot(phyloLeaf);
			} else if(currentPhyloObject instanceof PhyloBranch) {
				((PhyloBranch) currentPhyloObject).setSubtree(phyloLeaf);
			}
			currentPhyloObject = phyloLeaf;
		} else {
			throw new PhyloDocumentException(Code.UNKNOWN_KEY, objFieldName);
		}
	}

	@Override
	public void postVisitCommandObject(String objFieldName, CommandObject commandObject) {
		if(objFieldName.equals("userData")) {
			currentUserData = null;
		} else if(objFieldName.equals("root")) {
		} else if(objFieldName.equals("phyloTree")) {
		} else if(objFieldName.equals("branch")) {
		} else if(objFieldName.equals("internal") || objFieldName.equals("leaf")) {
			PhyloBranch parentPhyloBranch = ((PhyloSubtree<?>) currentPhyloObject).getParentPhyloBranch();
			if(parentPhyloBranch != null) {
				currentPhyloObject = parentPhyloBranch;
			} else {
				currentPhyloObject = ((PhyloSubtree<?>) currentPhyloObject).getTree();
			}
		} else {
			throw new PhyloDocumentException(Code.UNKNOWN_KEY, objFieldName);
		}
	}

	@Override
	public void preVisitCommandFieldValue(String objFieldName, CommandFieldValue commandFieldValue) {
		if(currentUserData != null) {
			if(commandFieldValue instanceof SimpleCommandValue) {
				currentUserData.put(objFieldName, ((SimpleCommandValue) commandFieldValue).getValue());
			} else {
				throw new PhyloDocumentException(Code.ILLEGAL_USER_DATA_VALUE, 
						objFieldName, commandFieldValue.getClass().getSimpleName());
			}
		}
	}

	@Override
	public void preVisitCommandArrayItem(String arrayFieldName, CommandArrayItem commandArrayItem) {
		if(arrayFieldName.equals("branch")) {
			PhyloInternal phyloInternal = (PhyloInternal) currentPhyloObject;
			PhyloBranch phyloBranch = new PhyloBranch();
			phyloInternal.addBranch(phyloBranch);
			currentPhyloObject = phyloBranch;
		} else {
			throw new PhyloDocumentException(Code.FORMAT_ERROR, "Unexpected array "+arrayFieldName);
		}
	}

	@Override
	public void postVisitCommandArrayItem(String arrayFieldName, CommandArrayItem commandArrayItem) {
		if(arrayFieldName.equals("branch")) {
			currentPhyloObject = ((PhyloBranch) currentPhyloObject).getParentPhyloInternal();
		}
	}

	
}
