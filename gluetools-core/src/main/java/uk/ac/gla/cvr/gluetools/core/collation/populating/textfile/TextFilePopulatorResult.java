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
package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class TextFilePopulatorResult extends BaseTableResult<Map<String,String>> {

	public static final String SOURCE_NAME = "sourceName";
	public static final String SEQUENCE_ID = "sequenceID";
	public static final String PROPERTY = "property";
	public static final String VALUE = "value";

	public TextFilePopulatorResult(List<Map<String, String>> rowObjects) {
		super("textFilePopulatorResult", rowObjects, column(SOURCE_NAME, x -> x.get(SOURCE_NAME)), 
				column(SEQUENCE_ID, x -> x.get(SEQUENCE_ID)), 
				column(PROPERTY, x -> x.get(PROPERTY)), 
				column(VALUE, x -> x.get(VALUE)));
	}

}
