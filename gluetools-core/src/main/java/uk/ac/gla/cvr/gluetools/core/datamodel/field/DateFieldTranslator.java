package uk.ac.gla.cvr.gluetools.core.datamodel.field;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldException.Code;

public class DateFieldTranslator extends FieldTranslator<Date> {

	private static final DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
	
	public DateFieldTranslator() {
		super(Date.class);
	}

	@Override
	public Date valueFromString(String string) {
		try {
			Date dateValue = dateFormat.parse(string);
			return dateValue;
		} catch (ParseException pe) {
			throw new FieldException(pe, Code.INCORRECT_VALUE_FORMAT, string, getValueClass().getSimpleName(), pe.getMessage());
		}
	}

	@Override
	public String valueToString(Date value) {
		return dateFormat.format(value);
	}

}
