package uk.ac.gla.cvr.gluetools.core.datafield;


public class BooleanField extends DataField<Boolean> {

	public BooleanField(String name) {
		super(name, Boolean.class);
	}

	@Override
	public DataFieldValue<Boolean> valueFromString(String string) {
		return new DataFieldValue<Boolean>(this, Boolean.parseBoolean(string));
	}


}
