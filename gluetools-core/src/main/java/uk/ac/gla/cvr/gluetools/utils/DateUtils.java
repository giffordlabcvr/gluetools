package uk.ac.gla.cvr.gluetools.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

public class DateUtils {

	public static final String GLUE_DATE_REGEX = "\\d{2}-[A-Za-z]{3}-\\d{4}";
	private static final DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
	
	public static String render(Date value) {
		return dateFormat.format(value);
	}
	
	public static boolean isDateString(String string) {
		return string.matches("^"+GLUE_DATE_REGEX+"$");
	}
	
	public static Date parse(String string) {
		if(!isDateString(string)) {
			throw new DateUtilsException(DateUtilsException.Code.DATE_PARSE_ERROR, string, "Incorrect format");
		}
		try {
			Date dateValue = dateFormat.parse(string);
			return dateValue;
		} catch (ParseException pe) {
			throw new DateUtilsException(pe, DateUtilsException.Code.DATE_PARSE_ERROR, string, pe.getLocalizedMessage());
		}
	}

	public static String formatDuration(long millis) {
		StringBuffer buf = new StringBuffer();
		Duration duration = Duration.ofMillis(millis);
		long days = duration.toDays();
		duration = duration.minusDays(days);
		long hours = duration.toHours();
		duration = duration.minusHours(hours);
		long minutes = duration.toMinutes();
		duration = duration.minusMinutes(minutes);
		long seconds = duration.getSeconds();
		duration = duration.minusSeconds(seconds);
		long milliseconds = duration.toMillis();
		duration = duration.minusMillis(milliseconds);
		
		if(days > 0) {
			buf.append(days+" days, ");
		}
		if(hours > 0 || buf.length()>0) {
			buf.append(hours+"h");
		}
		if(minutes > 0 || buf.length()>0) {
			buf.append(minutes+"m");
		}
		if(seconds > 0 || buf.length()>0) {
			buf.append(seconds+"s");
		}
		if(milliseconds > 0 || buf.length()>0) {
			buf.append(milliseconds+"ms");
		}
		return buf.toString();
	}

}
