package uk.ac.gla.cvr.gluetools.core.newick;

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;

public class NewickBootstrapsToPhyloTreeParser extends NewickToPhyloTreeParser {

	public NewickBootstrapsToPhyloTreeParser() {
		super(new NewickInterpreter() {
			@Override
			public void parseInternalName(PhyloInternal phyloInternal, String internalName) {
				phyloInternal.ensureUserData().put("bootstraps", Integer.parseInt(internalName));
			}
		});
	}

}
