package uk.ac.gla.cvr.gluetools.core.datamodel.field;

import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldException.Code;


public class DoubleFieldTranslator extends FieldTranslator<Double> {

	public DoubleFieldTranslator() {
		super(Double.class);
	}

	@Override
	public Double valueFromString(String string) {
		try {
			return Double.parseDouble(string);
		} catch(NumberFormatException nfe) {
			throw new FieldException(Code.INCORRECT_VALUE_FORMAT, string, getValueClass().getSimpleName(), "Not a double");
		}
	}

	@Override
	public String valueToString(Double value) {
		return Double.toString(value);
	}


}
