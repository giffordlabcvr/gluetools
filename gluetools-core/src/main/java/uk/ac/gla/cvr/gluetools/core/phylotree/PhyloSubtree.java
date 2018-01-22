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

public abstract class PhyloSubtree<C extends PhyloSubtree<?>> extends PhyloObject<C> {

	private PhyloBranch parentPhyloBranch;
	private PhyloTree tree;
	
	public PhyloBranch getParentPhyloBranch() {
		return parentPhyloBranch;
	}

	public void setParentPhyloBranch(PhyloBranch parentPhyloBranch) {
		this.parentPhyloBranch = parentPhyloBranch;
	}

	public String getName() {
		return (String) ensureUserData().get("name");		
	}

	public void setName(String name) {
		ensureUserData().put("name", name);		
	}

	public void setTree(PhyloTree tree) {
		this.tree = tree;
	}
	
	public PhyloTree getTree() {
		if(this.tree != null) {
			return this.tree;
		}
		return getParentPhyloBranch().getTree();
	}
	
	public abstract void accept(PhyloTreeVisitor visitor);


	
}
