package uk.ac.gla.cvr.gluetools.core.datafield;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorException;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorException.Code;

public class DateField extends DataField<Date> {

	private static final DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
	
	public DateField(String name) {
		super(name, Date.class);
	}

	@Override
	public FieldValue<Date> valueFromString(String string) {
		try {
			Date dateValue = dateFormat.parse(string);
			return new FieldValue<Date>(this, dateValue);
		} catch (ParseException pe) {
			throw new XmlPopulatorException(pe, Code.INCORRECT_VALUE_FORMAT, string, getValueClass().getSimpleName(), getName(), pe.getMessage());
		}
	}

	@Override
	public String valueToString(Date value) {
		return dateFormat.format(value);
	}

}
