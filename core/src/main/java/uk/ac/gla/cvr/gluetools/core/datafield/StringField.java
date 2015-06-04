package uk.ac.gla.cvr.gluetools.core.datafield;

public class StringField extends DataField<String> {

	public StringField(String name) {
		super(name, String.class);
	}

	@Override
	public DataFieldValue<String> valueFromString(String string) {
		return new DataFieldValue<String>(this, string);
	}

}
