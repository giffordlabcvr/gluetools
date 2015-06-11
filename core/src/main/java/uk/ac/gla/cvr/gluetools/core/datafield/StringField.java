package uk.ac.gla.cvr.gluetools.core.datafield;

public class StringField extends DataField<String> {

	public StringField(String name) {
		super(name, String.class);
	}

	@Override
	public FieldValue<String> valueFromString(String string) {
		return new FieldValue<String>(this, string);
	}

	@Override
	public String valueToString(String string) {
		return string;
	}

}
