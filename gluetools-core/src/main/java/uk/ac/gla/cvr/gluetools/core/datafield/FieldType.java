package uk.ac.gla.cvr.gluetools.core.datafield;

public enum FieldType {

	BOOLEAN(BooleanField.class), 
	DATE(DateField.class),
	VARCHAR(StringField.class),
	INTEGER(IntegerField.class);
	
	private Class<? extends DataField<?>> fieldClass;
	
	private FieldType(Class<? extends DataField<?>> fieldClass) {
		this.fieldClass = fieldClass;
	}

	public Class<? extends DataField<?>> getFieldClass() {
		return fieldClass;
	}
	
}
