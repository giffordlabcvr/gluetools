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
package uk.ac.gla.cvr.gluetools.core.datamodel.field;

import java.util.Date;

public enum FieldType {

	// need BIT here because of bug in cayenne <-> Derby mapping?
	BOOLEAN(new BooleanFieldTranslator(), Boolean.class.getCanonicalName(), "BIT"),  
	DATE(new DateFieldTranslator(), Date.class.getCanonicalName()),
	VARCHAR(new StringFieldTranslator(), String.class.getCanonicalName()),
	CLOB(new StringFieldTranslator(), String.class.getCanonicalName()),
	INTEGER(new IntegerFieldTranslator(), Integer.class.getCanonicalName()),
	DOUBLE(new DoubleFieldTranslator(), Double.class.getCanonicalName());
	
	private FieldTranslator<?> fieldTranslator;
	private String javaType;
	private String cayenneType;
	
	private FieldType(FieldTranslator<?> fieldTranslator, String javaType, String cayenneType) {
		this.fieldTranslator = fieldTranslator;
		this.javaType = javaType;
		this.cayenneType = cayenneType;
	}

	private FieldType(FieldTranslator<?> fieldTranslator, String javaType) {
		this(fieldTranslator, javaType, null);
	}

	public String cayenneType() {
		if(cayenneType != null) {
			return cayenneType;
		}
		return name();
	}
	
	public FieldTranslator<?> getFieldTranslator() {
		return fieldTranslator;
	}

	public String javaType() {
		return javaType;
	}
}
