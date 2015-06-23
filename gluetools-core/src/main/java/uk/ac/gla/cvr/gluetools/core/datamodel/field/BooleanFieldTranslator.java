package uk.ac.gla.cvr.gluetools.core.datamodel.field;


public class BooleanFieldTranslator extends FieldTranslator<Boolean> {

	public BooleanFieldTranslator() {
		super(Boolean.class);
	}

	@Override
	public Boolean valueFromString(String string) {
		return Boolean.parseBoolean(string);
	}

	@Override
	public String valueToString(Boolean value) {
		return Boolean.toString(value);
	}


}
