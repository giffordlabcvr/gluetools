package uk.ac.gla.cvr.gluetools.core.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class GlueLoggingFormatter extends Formatter {

	private static DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
	
	@Override
	public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        String sourceClassName = record.getSourceClassName();
        if(sourceClassName == null) {
        	sourceClassName = "UnknownClass";
        }
        int lastDotIndex = sourceClassName.lastIndexOf('.');
        if(lastDotIndex >= 0) {
        	sourceClassName = sourceClassName.substring(lastDotIndex+1);
        }
		sb.append(dateFormat.format(new Date(record.getMillis())))
            .append(" ")
        	.append(sourceClassName)
            .append(" ")
            .append(record.getLevel().getLocalizedName())
            .append(": ")
            .append(formatMessage(record));

        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
                // ignore
            }
        }
        return sb.toString();
     }

}
