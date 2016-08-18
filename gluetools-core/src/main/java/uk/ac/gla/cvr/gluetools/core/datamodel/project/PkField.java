package uk.ac.gla.cvr.gluetools.core.datamodel.project;


public class PkField {
	private String name;
	private String cayenneType;
	private Integer maxLength;
	
	public PkField(String name, String cayenneType, Integer maxLength) {
		super();
		this.name = name;
		this.cayenneType = cayenneType;
		this.maxLength = maxLength;
	}

	public String getName() {
		return name;
	}

	public String getCayenneType() {
		return cayenneType;
	}

	public Integer getMaxLength() {
		return maxLength;
	}
	
}
