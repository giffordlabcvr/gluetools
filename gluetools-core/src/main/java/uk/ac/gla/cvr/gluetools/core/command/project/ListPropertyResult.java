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
