package uk.ac.gla.cvr.gluetools.core.datamodel.field;

import java.util.Date;

public enum FieldType {

	BOOLEAN(new BooleanFieldTranslator(), Boolean.class.getCanonicalName()), 
	DATE(new DateFieldTranslator(), Date.class.getCanonicalName()),
	VARCHAR(new StringFieldTranslator(), String.class.getCanonicalName()),
	INTEGER(new IntegerFieldTranslator(), Integer.class.getCanonicalName())/*,
	BLOB(BlobField.class, "byte[]")*/;
	
	private FieldTranslator<?> fieldTranslator;
	private String javaType;
	
	private FieldType(FieldTranslator<?> fieldTranslator, String javaType) {
		this.fieldTranslator = fieldTranslator;
		this.javaType = javaType;
	}

	public FieldTranslator<?> getFieldTranslator() {
		return fieldTranslator;
	}

	public String javaType() {
		return javaType;
	}
}
