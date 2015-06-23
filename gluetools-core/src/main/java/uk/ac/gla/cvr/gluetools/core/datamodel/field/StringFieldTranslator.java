package uk.ac.gla.cvr.gluetools.core.datamodel.field;

public class StringFieldTranslator extends FieldTranslator<String> {

	public StringFieldTranslator() {
		super(String.class);
	}

	@Override
	public String valueFromString(String string) {
		return string;
	}

	@Override
	public String valueToString(String string) {
		return string;
	}

}
