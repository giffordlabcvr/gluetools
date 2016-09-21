package uk.ac.gla.cvr.gluetools.utils;

import java.util.Date;

public class GlueTypeUtils {

	public enum GlueType {
		Object {
			@Override
			public java.lang.String renderAsString(Object value) {
				throw new RuntimeException("Cannot render Object type as string");
			}
		}, 
		Integer {
			@Override
			public java.lang.String renderAsString(Object value) {
				return java.lang.Integer.toString((Integer) value);
			}
		},
		Double {
			@Override
			public java.lang.String renderAsString(Object value) {
				return java.lang.Double.toString((Integer) value);
			}
		},
		String {
			@Override
			public java.lang.String renderAsString(Object value) {
				return (String) value;
			}
		},
		Boolean {
			@Override
			public java.lang.String renderAsString(Object value) {
				return java.lang.Boolean.toString((Boolean) value);
			}
		},
		Date {
			@Override
			public java.lang.String renderAsString(Object value) {
				return DateUtils.render((Date) value);
			}
		},
		Null {
			@Override
			public java.lang.String renderAsString(Object value) {
				throw new RuntimeException("Cannot render Null type as string");
			}
		};

		public abstract String renderAsString(Object value);
	}

	public static Object typeStringToObject(GlueTypeUtils.GlueType glueType, String string) {
		switch(glueType) {
		case Double:
			return Double.parseDouble(string);
		case Integer:
			return Integer.parseInt(string);
		case Boolean:
			return Boolean.parseBoolean(string);
		case Date:
			return DateUtils.parse(string);
		case String:
			return string;
		case Null:
			return null;
		case Object:
			throw new RuntimeException("Cannot transform string to object for type Object");
		default:
			// maybe it could be a map?
			throw new RuntimeException("Unknown glueType: "+glueType);
		}
	}

	public static GlueType glueTypeFromObject(Object value) {
		if(value == null) {
			return GlueType.Null;
		} else if (value instanceof Double) {
			return GlueType.Double;
		} else if (value instanceof Integer) {
			return GlueType.Integer;
		} else if (value instanceof Boolean) {
			return GlueType.Boolean;
		}  else if (value instanceof Date) {
			return GlueType.Date;
		} else if (value instanceof String) {
			return GlueType.String;
		} else {
			throw new RuntimeException("Object "+value+" is not a GLUE type");
		}
	}


}
