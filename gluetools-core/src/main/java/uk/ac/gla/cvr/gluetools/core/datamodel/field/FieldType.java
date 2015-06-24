package uk.ac.gla.cvr.gluetools.core.datamodel.field;

import java.util.Date;

public enum FieldType {

	// need BIT here because of bug in cayenne <-> Derby mapping?
	BOOLEAN(new BooleanFieldTranslator(), Boolean.class.getCanonicalName(), "BIT"),  
	DATE(new DateFieldTranslator(), Date.class.getCanonicalName()),
	VARCHAR(new StringFieldTranslator(), String.class.getCanonicalName()),
	INTEGER(new IntegerFieldTranslator(), Integer.class.getCanonicalName())/*,
	BLOB(BlobField.class, "byte[]")*/;
	
	private FieldTranslator<?> fieldTranslator;
	private String javaType;
	private String cayenneType;
	
	private FieldType(FieldTranslator<?> fieldTranslator, String javaType, String cayenneType) {
		this.fieldTranslator = fieldTranslator;
		this.javaType = javaType;
		this.cayenneType = cayenneType;
	}

	private FieldType(FieldTranslator<?> fieldTranslator, String javaType) {
		this(fieldTranslator, javaType, null);
	}

	public String cayenneType() {
		if(cayenneType != null) {
			return cayenneType;
		}
		return name();
	}
	
	public FieldTranslator<?> getFieldTranslator() {
		return fieldTranslator;
	}

	public String javaType() {
		return javaType;
	}
}
