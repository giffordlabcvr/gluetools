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
package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public class ListPropertyResult extends TableResult {

	public ListPropertyResult(CommandContext cmdContext, List<String> propertyPaths, GlueDataObject glueDataObject) {
		super("listProperty", Arrays.asList("property", "value"), getRowData(cmdContext, propertyPaths, glueDataObject));
	}
	
	private static List<Map<String, Object>> getRowData(CommandContext cmdContext, List<String> propertyPaths, GlueDataObject glueDataObject) {
		List<Map<String, Object>> rowData = new ArrayList<Map<String,Object>>();
		for(String propertyPath: propertyPaths) {
			Map<String, Object> row = new LinkedHashMap<String, Object>();
			row.put("property", propertyPath);
			row.put("value", ListResult.generateResultValue(cmdContext, glueDataObject, propertyPath));
			rowData.add(row);
		}
		return rowData;
	}
}
