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

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.newick.PhyloNewickException.Code;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;

public class NewickTransferBootstrapsToPhyloTreeParser extends NewickToPhyloTreeParser {

	public NewickTransferBootstrapsToPhyloTreeParser() {
		super(new NewickInterpreter() {
			@Override
			public void parseInternalName(PhyloInternal phyloInternal, String internalName) {
				PhyloBranch parentBranch = phyloInternal.getParentPhyloBranch();
				if(parentBranch == null) {
					// changed this to a warning to work around possible bug in RAXML-NG
					// throw new PhyloNewickException(Code.FORMAT_ERROR, "NEWICK_TRANSFER_BOOTSTRAPS format should not try to annotate a bootstraps value as an internal name of the root node");
					GlueLogger.getGlueLogger().warning("Ignoring transfer bootstrap value annotated as an internal name of the root node");
					return;
				}
				Double transferBootstraps;
				try {
					transferBootstraps = Double.parseDouble(internalName);
				} catch(NumberFormatException nfe) {
					throw new PhyloNewickException(Code.FORMAT_ERROR, "NEWICK_TRANSFER_BOOTSTRAPS internal node names should be floating point numbers");
				}
				if(transferBootstraps < 0 || transferBootstraps > 1.0) {
					throw new PhyloNewickException(Code.FORMAT_ERROR, "NEWICK_TRANSFER_BOOTSTRAPS internal node names should be between 0 and 1.0 inclusive");
				};
				
				Map<String, Object> parentBranchUserData = parentBranch.ensureUserData();
				Double existingValue = (Double) parentBranchUserData.get("transferBootstraps");
				if(existingValue != null && !existingValue.equals(transferBootstraps)) {
					throw new PhyloNewickException(Code.FORMAT_ERROR, "NEWICK_TRANSFER_BOOTSTRAPS internal node names of the children of the root node should be equal as they are transfer bootstraps for the same edge");					
				} 
				parentBranchUserData.put("transferBootstraps", transferBootstraps);
			}
		});
	}
	
}
