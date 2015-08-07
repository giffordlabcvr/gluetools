package uk.ac.gla.cvr.gluetools.core.command.result;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.DocumentBuilder;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

import com.brsanthu.dataexporter.model.AlignType;
import com.brsanthu.dataexporter.model.Row;
import com.brsanthu.dataexporter.model.StringColumn;
import com.brsanthu.dataexporter.output.texttable.TextTableExportOptions;
import com.brsanthu.dataexporter.output.texttable.TextTableExportStyle;
import com.brsanthu.dataexporter.output.texttable.TextTableExporter;

public class TableResult extends CommandResult {

	public static final String IS_NULL = "isNull";
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
		Element docElem = getDocument().getDocumentElement();
		List<String> headers = getColumnHeaders();
		int index = headers.indexOf(columnName)+1; // XPath indices start from 1
		List<Element> valueElems = GlueXmlUtils.getXPathElements(docElem, ROW+"/value["+index+"]");
		return valueElems.stream().map(elem -> {
			if(JsonUtils.getJsonType(elem) == JsonType.Null) {
				return null;
			} else {
				return elem.getTextContent();
			}
		}).collect(Collectors.toList());
	}


	public List<Map<String, Object>> asListOfMaps(List<String> headers) {
		Element docElem = getDocument().getDocumentElement();
		List<Element> objElems = GlueXmlUtils.findChildElements(docElem, ROW);
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		for(Element objElem: objElems) {
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			List<Element> valueElems = GlueXmlUtils.findChildElements(objElem, VALUE);
			for(int i = 0; i < headers.size(); i++) {
				String header = headers.get(i);
				Element valueElem = valueElems.get(i);
				map.put(header, JsonUtils.elementToObject(valueElem));
			}
			results.add(map);
		}
		return results;
	}
	
	public List<Map<String, Object>> asListOfMaps() {
		return asListOfMaps(getColumnHeaders());
	}

	public List<String> getColumnHeaders() {
		Element docElem = getDocument().getDocumentElement();
		List<String> headers = GlueXmlUtils.findChildElements(docElem, COLUMN).stream().
				map(Element::getTextContent).collect(Collectors.toList());
		return headers;
	}

	public void renderToStringWriter(StringWriter stringWriter,
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


}
