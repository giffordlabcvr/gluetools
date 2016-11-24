package uk.ac.gla.cvr.gluetools.core.newick;

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;

public class PhyloTreeToNewickBootstrapsGenerator extends PhyloTreeToNewickGenerator {

	public PhyloTreeToNewickBootstrapsGenerator() {
		super(new NewickGenerator() {
			@Override
			public String generateInternalName(PhyloInternal phyloInternal) {
				Integer bootstraps = (Integer) phyloInternal.ensureUserData().get("bootstraps");
				if(bootstraps != null) {
					return Integer.toString(bootstraps); 
				}
				return null;
			}
		});
	}

}
