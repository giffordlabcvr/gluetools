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
package uk.ac.gla.cvr.gluetools.core.plugins;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class PluginConfigException extends GlueException {

	public enum Code implements GlueErrorCode {
		CONFIG_CONSTRAINT_VIOLATION("errorTxt"), 
		REQUIRED_CONFIG_MISSING("xPath"), 
		TOO_MANY_CONFIG_ELEMENTS("xPath", "numFound", "maximum"), 
		TOO_FEW_CONFIG_ELEMENTS("xPath", "numFound", "minimum"),
		CONFIG_FORMAT_ERROR("xPath", "errorTxt", "value"),
		UNKNOWN_CONFIG_ELEMENT("xPath"), 
		UNKNOWN_CONFIG_ATTRIBUTE("xPath"), 
		REQUIRED_PROPERTY_MISSING("elementName", "propertyName"), 
		PROPERTY_FORMAT_ERROR("propertyName", "errorTxt", "value"), 
		PROPERTY_VALUE_OUT_OF_RANGE("propertyName", "value", "operator", "threshold"), 
		MULTIPLE_PROPERTY_SETTINGS("propertyName"),
		TOO_MANY_PROPERTY_VALUES("propertyName", "numFound", "maximum"), 
		TOO_FEW_PROPERTY_VALUES("propertyName", "numFound", "minimum"),
		COMMAND_DOCUMENT_PROPERTY_ERROR("errorTxt");

		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
		
	}
	
	public PluginConfigException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public PluginConfigException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
