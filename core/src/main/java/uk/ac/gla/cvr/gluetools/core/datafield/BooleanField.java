package uk.ac.gla.cvr.gluetools.core.datafield;


public class BooleanField extends DataField<Boolean> {

	public BooleanField(String name) {
		super(name, Boolean.class);
	}

	@Override
	public FieldValue<Boolean> valueFromString(String string) {
		return new FieldValue<Boolean>(this, Boolean.parseBoolean(string));
	}


}
