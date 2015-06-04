package uk.ac.gla.cvr.gluetools.core.datafield;

public abstract class DataField<T extends Object> {

	private String name;
	private Class<T> valueClass;

	public String getName() {
		return name;
	}

	public DataField(String name, Class<T> valueClass) {
		super();
		this.name = name;
		this.valueClass = valueClass;
	}
	
	public abstract DataFieldValue<T> valueFromString(String string);

	public Class<T> getValueClass() {
		return valueClass;
	}
	
	
}
