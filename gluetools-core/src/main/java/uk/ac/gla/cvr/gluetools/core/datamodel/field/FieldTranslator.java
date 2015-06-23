package uk.ac.gla.cvr.gluetools.core.datamodel.field;

public abstract class FieldTranslator<T extends Object> {

	private Class<T> valueClass;

	public FieldTranslator(Class<T> valueClass) {
		super();
		this.valueClass = valueClass;
	}
	
	public abstract T valueFromString(String string);

	public abstract String valueToString(T value);
	
	public Class<T> getValueClass() {
		return valueClass;
	}
	
}
