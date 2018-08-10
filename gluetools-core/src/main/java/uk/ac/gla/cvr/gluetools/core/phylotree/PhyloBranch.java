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
package uk.ac.gla.cvr.gluetools.core.phylotree;

import java.math.BigDecimal;


public class PhyloBranch extends PhyloObject<PhyloBranch> {

	private PhyloInternal parentPhyloInternal; // phyloInternal for now (later may allow PhlyoTree?)
	private PhyloSubtree<?> subtree;
	private int childBranchIndex;
	
	public int getChildBranchIndex() {
		return childBranchIndex;
	}

	public void setChildBranchIndex(int childBranchIndex) {
		this.childBranchIndex = childBranchIndex;
	}

	public PhyloSubtree<?> getSubtree() {
		return subtree;
	}

	public String getBranchLabel() {
		return (String) ensureUserData().get("label");
	}

	public void setBranchLabel(String branchLabel) {
		ensureUserData().put("label", branchLabel);		
	}

	public void setSubtree(PhyloSubtree<?> subtree) {
		this.subtree = subtree;
		subtree.setParentPhyloBranch(this);
	}
	
	public BigDecimal getLength() {
		return new BigDecimal((String) ensureUserData().get("length"));
	}

	public void setLength(BigDecimal length) {
		ensureUserData().put("length", length.toString());
	}

	public String getComment() {
		return (String) ensureUserData().get("comment");
	}

	public void setComment(String comment) {
		ensureUserData().put("comment", comment);		
	}
	
	public PhyloInternal getParentPhyloInternal() {
		return parentPhyloInternal;
	}

	void setParentPhyloInternal(PhyloInternal parentPhyloInternal) {
		this.parentPhyloInternal = parentPhyloInternal;
	}

	public void accept(int branchIndex, PhyloTreeVisitor visitor) {
		visitor.preVisitBranch(branchIndex, this);
		subtree.accept(visitor);
		visitor.postVisitBranch(branchIndex, this);
	}

	
	
	@Override
	public PhyloBranch clone() {
		PhyloBranch phyloBranch = new PhyloBranch();
		copyPropertiesTo(phyloBranch);
		return phyloBranch;
	}

	public PhyloTree getTree() {
		return getParentPhyloInternal().getTree();
	}

	public PhyloSubtree<?> otherSubtree(PhyloSubtree<?> thisSubtree) {
		if(thisSubtree == parentPhyloInternal) {
			return subtree;
		} else if(thisSubtree == subtree) {
			return parentPhyloInternal;
		}
		throw new RuntimeException("invalid use of PhyloBranch.otherSubtree");
	}
	
	
}
