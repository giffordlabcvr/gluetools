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

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public interface NewickInterpreter extends Plugin {

	/* floating point number associated with branches */
	public default void parseBranchLength(PhyloBranch phyloBranch, String branchLength) {
		phyloBranch.ensureUserData().put("length", branchLength);
	}

	/* main string associated with leaf nodes */
	public default void parseLeafName(PhyloLeaf phyloLeaf, String leafName) {
		phyloLeaf.ensureUserData().put("name", leafName);
	}

	/* main string associated with internal nodes. Appears after ")" */
	public default void parseInternalName(PhyloInternal phyloInternal, String internalName) {
		phyloInternal.ensureUserData().put("name", internalName);
	}

	/* optional string within [] associated with branches */
	public default void parseBranchComment(PhyloBranch phyloBranch, String comment) {
		phyloBranch.ensureUserData().put("comment", comment);
	}

	/* optional string within {}, associated with branches */
	public default void parseBranchLabel(PhyloBranch phyloBranch, String label) {
		phyloBranch.ensureUserData().put("label", label);
	}
	
}
