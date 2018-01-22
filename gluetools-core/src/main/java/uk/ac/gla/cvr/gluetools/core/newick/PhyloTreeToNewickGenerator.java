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
package uk.ac.gla.cvr.gluetools.core.newick;

import java.util.Optional;

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;

public class PhyloTreeToNewickGenerator implements PhyloTreeVisitor {
	
	private NewickGenerator newickGenerator;
	private StringBuffer buf = new StringBuffer();
	
	public PhyloTreeToNewickGenerator() {
		this(new NewickGenerator() {});
	}

	public PhyloTreeToNewickGenerator(NewickGenerator newickGenerator) {
		super();
		this.newickGenerator = newickGenerator;
	}

	
	public String getNewickString() {
		return buf.toString();
	}

	@Override
	public void preVisitTree(PhyloTree phyloTree) {
	}

	@Override
	public void postVisitTree(PhyloTree phyloTree) {
		buf.append(";");
	}

	@Override
	public void preVisitInternal(PhyloInternal phyloInternal) {
		buf.append("(");
	}

	@Override
	public void postVisitInternal(PhyloInternal phyloInternal) {
		buf.append(")");
		Optional.ofNullable(newickGenerator.generateInternalName(phyloInternal)).ifPresent(name -> buf.append(name));
	}

	@Override
	public void visitLeaf(PhyloLeaf phyloLeaf) {
		Optional.ofNullable(newickGenerator.generateLeafName(phyloLeaf)).ifPresent(name -> buf.append(name));
	}

	@Override
	public void preVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
		if(branchIndex > 0) {
			buf.append(",");
		}
	}

	@Override
	public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
		Optional.ofNullable(newickGenerator.generateBranchLength(phyloBranch)).ifPresent(length -> 
		buf.append(":").append(length.toString()));
		Optional.ofNullable(newickGenerator.generateBranchComment(phyloBranch)).ifPresent(comment -> 
		buf.append("[").append(comment).append("]"));
		Optional.ofNullable(newickGenerator.generateBranchLabel(phyloBranch)).ifPresent(branchLabel -> 
		buf.append("{").append(branchLabel).append("}"));
	}

	
}