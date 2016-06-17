package uk.ac.gla.cvr.gluetools.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

	private static final DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
	
	public static String render(Date value) {
		return dateFormat.format(value);
	}
	
	public static boolean isDateString(String string) {
		return string.matches("^\\d{2}-[A-Za-z]{3}-\\d{4}$");
	}
	
	public static Date parse(String string) {
		try {
			Date dateValue = dateFormat.parse(string);
			return dateValue;
		} catch (ParseException pe) {
			throw new DateUtilsException(pe, DateUtilsException.Code.DATE_PARSE_ERROR, pe.getLocalizedMessage());
		}
	}


}
