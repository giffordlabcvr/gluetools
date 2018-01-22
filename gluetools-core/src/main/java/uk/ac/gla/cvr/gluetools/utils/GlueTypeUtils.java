/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
				return java.lang.Double.toString((Double) value);
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
