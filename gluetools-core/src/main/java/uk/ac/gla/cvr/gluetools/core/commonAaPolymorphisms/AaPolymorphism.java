package uk.ac.gla.cvr.gluetools.core.commonAaPolymorphisms;

public class AaPolymorphism {

	private String refName;
	private String featureName;
	private String variationName;
	private String variationDisplayName;
	private String codonLabel;
	private String refAa;
	private String variationAa;
	private String description;
	
	public AaPolymorphism(String refName, String featureName,
			String variationName, String variationDisplayName, String codonLabel, String refAa,
			String variationAa, String description) {
		super();
		this.refName = refName;
		this.featureName = featureName;
		this.variationName = variationName;
		this.variationDisplayName = variationDisplayName;
		this.codonLabel = codonLabel;
		this.refAa = refAa;
		this.variationAa = variationAa;
		this.description = description;
	}

	public String getRefName() {
		return refName;
	}

	public String getFeatureName() {
		return featureName;
	}

	public String getVariationName() {
		return variationName;
	}

	public String getCodonLabel() {
		return codonLabel;
	}

	public String getRefAa() {
		return refAa;
	}

	public String getVariationAa() {
		return variationAa;
	}

	public String getDescription() {
		return description;
	}

	public String getRegex() {
		if(variationAa.equals("*")) {
			return "\\*";
		}
		return variationAa;
	}

	public String getVariationDisplayName() {
		return variationDisplayName;
	}
	
}
