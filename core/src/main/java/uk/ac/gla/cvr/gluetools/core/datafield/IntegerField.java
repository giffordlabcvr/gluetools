package uk.ac.gla.cvr.gluetools.core.datafield;

import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulatorException;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulatorException.Code;

public class IntegerField extends DataField<Integer> {

	public IntegerField(String name) {
		super(name, Integer.class);
	}

	@Override
	public DataFieldValue<Integer> valueFromString(String string) {
		try {
			return new DataFieldValue<Integer>(this, Integer.parseInt(string));
		} catch(NumberFormatException nfe) {
			throw new DataFieldPopulatorException(nfe, Code.INCORRECT_VALUE_FORMAT, string, getValueClass().getSimpleName(), getName(), nfe.getMessage());
		}
	}


}
