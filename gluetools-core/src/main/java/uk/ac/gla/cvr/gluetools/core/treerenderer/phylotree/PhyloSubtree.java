package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class PhyloSubtree {

	private String name;
	
	private Map<String, Object> userData;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Map<String, Object> getUserData() {
		return userData;
	}

	public void setUserData(Map<String, Object> userData) {
		this.userData = userData;
	}

	public abstract void accept(PhyloTreeVisitor visitor);
	
}
