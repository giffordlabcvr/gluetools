package uk.ac.gla.cvr.gluetools.core.datafield;

public class DataFieldValue<T> {

	private DataField<T> dataField;
	private T value;
	
	public DataFieldValue(DataField<T> dataField, T value) {
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
	
}
