package uk.ac.gla.cvr.gluetools.core.datamodel.field;

import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldException.Code;


public class IntegerFieldTranslator extends FieldTranslator<Integer> {

	public IntegerFieldTranslator() {
		super(Integer.class);
	}

	@Override
	public Integer valueFromString(String string) {
		try {
			return Integer.parseInt(string);
		} catch(NumberFormatException nfe) {
			throw new FieldException(Code.INCORRECT_VALUE_FORMAT, string, getValueClass().getSimpleName(), "Not an integer");
		}
	}

	@Override
	public String valueToString(Integer value) {
		return Integer.toString(value);
	}


}
