package uk.ac.gla.cvr.gluetools.core.command.result;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
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

// TODO -- default list command for web interface should return only IDs
// TODO -- some kind of pagination for list commands.

public class ListResult extends CommandResult {

	
	public static final String IS_NULL = "isNull";
	public static final String VALUE = "value";
	public static final String OBJECT = "object";
	public static final String COLUMN = "column";
	public static final String LIST_RESULT = "listResult";
	public static final String OBJECT_TYPE = "objectType";

	public <D extends GlueDataObject> ListResult(Class<D> objectClass, List<D> results, List<String> columns) {
		super(LIST_RESULT);
		DocumentBuilder builder = getDocumentBuilder();
		if(columns == null) {
			columns = Arrays.asList(objectClass.getAnnotation(GlueDataClass.class).defaultListColumns());
		}
		builder.set(OBJECT_TYPE, objectClass.getSimpleName());
		ArrayBuilder columnsArrayBuilder = builder.setArray(COLUMN);
		for(String column: columns) {
			columnsArrayBuilder.add(column);
		}
		ArrayBuilder objectArrayBuilder = builder.setArray(OBJECT);
		for(D object: results) {
			ArrayBuilder valueArrayBuilder = objectArrayBuilder.addObject().setArray(VALUE);
			for(String column: columns) {
				Object columnValue = object.readNestedProperty(column);
				valueArrayBuilder.add(columnValue);
			}
		}
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringWriter stringWriter = new StringWriter();
		Element docElem = getDocument().getDocumentElement();
		String objectType = GlueXmlUtils.getXPathElement(docElem, OBJECT_TYPE).getTextContent();
		List<String> headers = GlueXmlUtils.findChildElements(docElem, COLUMN).stream().
				map(Element::getTextContent).collect(Collectors.toList());
		List<Element> objElems = GlueXmlUtils.findChildElements(docElem, OBJECT);
		int numFound = objElems.size();
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
			for(Element objElem: objElems) {
				Row row = new Row();
				List<Element> valueElems = GlueXmlUtils.findChildElements(objElem, VALUE);
				valueElems.forEach(valueElem -> {
					String cString;
					if(JsonUtils.getJsonType(valueElem) == JsonType.Null) {
						cString = "-";
					} else {
						cString = valueElem.getTextContent();
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
		renderCtx.output(stringWriter.toString()+objectType+"s found: "+numFound);
	}
	
	public List<String> getColumnValues(String columnName) {
		Element docElem = getDocument().getDocumentElement();
		List<String> headers = GlueXmlUtils.findChildElements(docElem, COLUMN).stream().
				map(Element::getTextContent).collect(Collectors.toList());
		int index = headers.indexOf(columnName)+1; // XPath indices start from 1
		List<Element> valueElems = GlueXmlUtils.getXPathElements(docElem, OBJECT+"/value["+index+"]");
		return valueElems.stream().map(elem -> {
			if(JsonUtils.getJsonType(elem) == JsonType.Null) {
				return null;
			} else {
				return elem.getTextContent();
			}
		}).collect(Collectors.toList());
	}


	public List<Map<String, Object>> asListOfMaps() {
		Element docElem = getDocument().getDocumentElement();
		List<String> headers = GlueXmlUtils.findChildElements(docElem, COLUMN).stream().
				map(Element::getTextContent).collect(Collectors.toList());
		List<Element> objElems = GlueXmlUtils.findChildElements(docElem, OBJECT);
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

}
