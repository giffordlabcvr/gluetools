package uk.ac.gla.cvr.gluetools.core.commonAaAnalyser;

import java.util.ArrayList;
import java.util.List;

public class CommonAminoAcids {

	private String refName;
	private String featureName;
	private String codonLabel;
	private List<String> commonAas;
	
	public CommonAminoAcids(String refName, String featureName, String codonLabel) {
		super();
		this.refName = refName;
		this.featureName = featureName;
		this.codonLabel = codonLabel;
		this.commonAas = new ArrayList<String>();
	}

	public String getRefName() {
		return refName;
	}

	public String getFeatureName() {
		return featureName;
	}

	public String getCodonLabel() {
		return codonLabel;
	}

	public List<String> getCommonAas() {
		return commonAas;
	}
	
}
