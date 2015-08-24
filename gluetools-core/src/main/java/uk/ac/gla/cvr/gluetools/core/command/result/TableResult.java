package uk.ac.gla.cvr.gluetools.core.command.result;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ArrayReader;
import uk.ac.gla.cvr.gluetools.core.document.DocumentBuilder;
import uk.ac.gla.cvr.gluetools.core.document.DocumentReader;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;

import com.brsanthu.dataexporter.model.AlignType;
import com.brsanthu.dataexporter.model.Row;
import com.brsanthu.dataexporter.model.StringColumn;
import com.brsanthu.dataexporter.output.texttable.TextTableExportOptions;
import com.brsanthu.dataexporter.output.texttable.TextTableExportStyle;
import com.brsanthu.dataexporter.output.texttable.TextTableExporter;

public class TableResult extends CommandResult {

	public static final String VALUE = "value";
	public static final String ROW = "row";
	public static final String COLUMN = "column";

	
	public TableResult(String rootObjectName, List<String> columnHeaders, List<Map<String, Object>> rowData) {
		super(rootObjectName);
		DocumentBuilder builder = getDocumentBuilder();
		ArrayBuilder columnsArrayBuilder = builder.setArray(COLUMN);
		for(String column: columnHeaders) {
			columnsArrayBuilder.add(column);
		}
		ArrayBuilder objectArrayBuilder = builder.setArray(ROW);
		for(Map<String, Object> row: rowData) {
			ArrayBuilder valueArrayBuilder = objectArrayBuilder.addObject().setArray(VALUE);
			for(String columnHeader: columnHeaders) {
				valueArrayBuilder.add(row.get(columnHeader));
			}
		}

	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		List<String> columnHeaders = getColumnHeaders();
		List<Map<String, Object>> listOfMaps = asListOfMaps(columnHeaders);
		if(listOfMaps.size() == 0) {
			renderCtx.output("Empty result table");
		} else {
			StringWriter stringWriter = new StringWriter();
			renderToStringWriter(stringWriter, columnHeaders, listOfMaps);
			renderCtx.output(stringWriter.toString());
		}
	}
	
	public List<String> getColumnValues(String columnName) {
		List<String> headers = getColumnHeaders();
		int index = headers.indexOf(columnName);
		List<String> values = new ArrayList<String>();
		DocumentReader documentReader = getDocumentReader();
		ArrayReader rowsReader = documentReader.getArray(ROW);
		for(int i = 0; i < rowsReader.size(); i++) {
			ObjectReader rowReader = rowsReader.getObject(i);
			ArrayReader valuesReader = rowReader.getArray(VALUE);
			Object value = valuesReader.value(index);
			if(value == null) {
				values.add(null);
			} else {
				values.add(value.toString());
			}
		}
		return values;
	}
	
	


	public List<Map<String, Object>> asListOfMaps(List<String> headers) {
		DocumentReader documentReader = getDocumentReader();
		ArrayReader rowsReader = documentReader.getArray(ROW);
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		for(int i = 0; i < rowsReader.size(); i++) {
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			ObjectReader rowReader = rowsReader.getObject(i);
			ArrayReader valuesReader = rowReader.getArray(VALUE);
			for(int j = 0; j < headers.size(); j++) {
				String header = headers.get(j);
				map.put(header, valuesReader.value(j));
			}
			results.add(map);
		}
		return results;
	}
	
	public List<Map<String, Object>> asListOfMaps() {
		return asListOfMaps(getColumnHeaders());
	}

	public List<String> getColumnHeaders() {
		ArrayReader columnsReader = getDocumentReader().getArray(COLUMN);
		List<String> headers = new ArrayList<String>();
		for(int i = 0; i < columnsReader.size(); i++) {
			headers.add(columnsReader.stringValue(i));
		}
		return headers;
	}

	public static void renderToStringWriter(StringWriter stringWriter,
			List<String> headers, List<Map<String, Object>> listOfMaps) {
		int numFound = listOfMaps.size();
		if(numFound > 0) {
			final int minColWidth = 25;
			TextTableExportOptions options = new TextTableExportOptions();
			options.setStyle(TextTableExportStyle.CLASSIC);
			TextTableExporter textTable = new TextTableExporter(stringWriter);
			for(String header: headers) {
				StringColumn column = new StringColumn(header, Math.max(minColWidth, header.length()));
				column.setAlign(AlignType.TOP_LEFT);
				textTable.addColumns(column);
			}
			for(Map<String, Object> map: listOfMaps) {
				Row row = new Row();
				headers.forEach(header -> {
					String cString;
					Object value = map.get(header);
					if(value == null) {
						cString = "-";
					} else {
						cString = value.toString();
					}
					if(cString.length() < minColWidth) {
						cString = " "+cString;
					}
					row.addCellValue(cString);
				});
				textTable.addRows(row);
			}
			textTable.finishExporting();
		} 
	}

	public static <D extends GlueDataObject> List<Map<String, Object>> listOfMapsFromDataObjects(
			List<D> results, List<String> propertyPaths) {
		List<Map<String, Object>> listOfMaps = new ArrayList<Map<String, Object>>();
		for(D object: results) {
			listOfMaps.add(MapResult.mapFromDataObject(propertyPaths, object));
		}
		return listOfMaps;
	}

	


}
