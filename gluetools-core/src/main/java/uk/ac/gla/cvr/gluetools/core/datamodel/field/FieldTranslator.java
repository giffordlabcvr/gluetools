package uk.ac.gla.cvr.gluetools.core.datamodel.field;

public abstract class FieldTranslator<T extends Object> {

	private Class<T> valueClass;

	public FieldTranslator(Class<T> valueClass) {
		super();
		this.valueClass = valueClass;
	}
	
	public abstract T valueFromString(String string);

	public abstract String valueToString(T value);
	
	@SuppressWarnings("unchecked")
	public final String objectValueToString(Object obj) {
		return valueToString((T) obj);
	}
	
	public Class<T> getValueClass() {
		return valueClass;
	}
	
}
