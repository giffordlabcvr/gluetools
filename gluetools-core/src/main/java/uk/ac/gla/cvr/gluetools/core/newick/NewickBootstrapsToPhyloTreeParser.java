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

import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.newick.PhyloNewickException.Code;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;

public class NewickBootstrapsToPhyloTreeParser extends NewickToPhyloTreeParser {

	public NewickBootstrapsToPhyloTreeParser() {
		super(new NewickInterpreter() {
			@Override
			public void parseInternalName(PhyloInternal phyloInternal, String internalName) {
				PhyloBranch parentBranch = phyloInternal.getParentPhyloBranch();
				if(parentBranch == null) {
					throw new PhyloNewickException(Code.FORMAT_ERROR, "NEWICK_BOOTSTRAPS format should not try to annotate a bootstraps value as an internal name of the root node");
				}
				Integer bootstraps;
				try {
					bootstraps = Integer.parseInt(internalName);
				} catch(NumberFormatException nfe) {
					throw new PhyloNewickException(Code.FORMAT_ERROR, "NEWICK_BOOTSTRAPS internal node names should be integers");
				}
				if(bootstraps < 0 || bootstraps > 100) {
					throw new PhyloNewickException(Code.FORMAT_ERROR, "NEWICK_BOOTSTRAPS internal node names should be between 0 and 100 inclusive");
				};
				
				Map<String, Object> parentBranchUserData = parentBranch.ensureUserData();
				Integer existingValue = (Integer) parentBranchUserData.get("bootstraps");
				if(existingValue != null && !existingValue.equals(bootstraps)) {
					throw new PhyloNewickException(Code.FORMAT_ERROR, "NEWICK_BOOTSTRAPS internal node names of the children of the root node should be equal as they are bootstraps for the same edge");					
				} 
				parentBranchUserData.put("bootstraps", bootstraps);
			}
		});
	}
	
}
