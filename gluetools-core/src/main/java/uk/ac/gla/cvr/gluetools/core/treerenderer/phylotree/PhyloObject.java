package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class PhyloObject {

	private Map<String, Object> userData;
	
	public Map<String, Object> getUserData() {
		return userData;
	}

	public void setUserData(Map<String, Object> userData) {
		this.userData = userData;
	}

	public Map<String, Object> ensureUserData() {
		if(this.userData == null) {
			this.userData = new LinkedHashMap<String, Object>();
		}
		return this.userData;
	}


}