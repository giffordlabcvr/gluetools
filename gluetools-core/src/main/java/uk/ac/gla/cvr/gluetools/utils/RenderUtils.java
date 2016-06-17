package uk.ac.gla.cvr.gluetools.utils;

import java.util.Date;

public class RenderUtils {

	private static RenderContext defaultRenderContext = new RenderContext(){};
	
	public static String render(Object value) {
		return render(value, defaultRenderContext);
	}
		
	public static String render(Object value, RenderContext renderContext) {
		String cString;
		if(value == null) {
			cString = "-";
		} else if(value instanceof Double) {
			Integer precision = renderContext.floatDecimalPlacePrecision();
			if(precision != null) {
				cString = String.format("%."+precision.intValue()+"f", ((Double) value).doubleValue());
			} else {
				cString = value.toString();
			}
		} else if(value instanceof Date) {
			cString = DateUtils.render((Date) value);
		} else {
			cString = value.toString();
		}
		return cString;
	}
	
}
