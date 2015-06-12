package uk.ac.gla.cvr.gluetools.core.datafield;

public class FieldValue<T> {

	private DataField<T> dataField;
	private T value;
	
	public FieldValue(DataField<T> dataField, T value) {
		super();
		this.dataField = dataField;
		this.value = value;
	}

	public DataField<T> getDataField() {
		return dataField;
	}

	public T getValue() {
		return value;
	}
	
	public String toString() {
		return dataField.valueToString(value);
	}
	
}
