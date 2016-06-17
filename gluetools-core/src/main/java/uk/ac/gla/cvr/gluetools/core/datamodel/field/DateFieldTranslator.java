package uk.ac.gla.cvr.gluetools.core.datamodel.field;

import java.util.Date;

import uk.ac.gla.cvr.gluetools.utils.DateUtils;

public class DateFieldTranslator extends FieldTranslator<Date> {

	public DateFieldTranslator() {
		super(Date.class);
	}

	@Override
	public Date valueFromString(String string) {
		return DateUtils.parse(string);
	}

	@Override
	public String valueToString(Date value) {
		return DateUtils.render(value);
	}


}
