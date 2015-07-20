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
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

import com.brsanthu.dataexporter.model.AlignType;
import com.brsanthu.dataexporter.model.Row;
import com.brsanthu.dataexporter.model.StringColumn;
import com.brsanthu.dataexporter.output.texttable.TextTableExportOptions;
import com.brsanthu.dataexporter.output.texttable.TextTableExportStyle;
import com.brsanthu.dataexporter.output.texttable.TextTableExporter;

// TODO -- default list command for web interface should return only IDs
// TODO -- some kind of pagination for list commands.

public class ListResult extends DocumentResult {

	
	public static final String NULL = "null";
	public static final String VALUE = "value";
	public static final String OBJECT = "object";
	public static final String COLUMN = "column";
	public static final String LIST_RESULT = "listResult";
	public static final String OBJECT_TYPE = "objectType";

	public <D extends GlueDataObject> ListResult(Class<D> objectClass, List<D> results, List<String> columns) {
		super(XmlUtils.documentWithElement(LIST_RESULT).getOwnerDocument());
		Element docElem = getDocument().getDocumentElement();
		XmlUtils.appendElementWithText(docElem, 
				OBJECT_TYPE, objectClass.getSimpleName());
		for(String column: columns) {
			XmlUtils.appendElementWithText(docElem, COLUMN, column);
		}
		for(D object: results) {
			Element rowElem = XmlUtils.appendElement(docElem, OBJECT);
			for(String column: columns) {
				Object columnValue = object.readNestedProperty(column);
				if(columnValue == null) {
					XmlUtils.appendElement(rowElem, NULL);
				} else {
					String valueText = columnValue.toString();
					XmlUtils.appendElementWithText(rowElem, VALUE, valueText);
				}
			}
		}
	}

	public <D extends GlueDataObject> ListResult(Class<D> objectClass, List<D> results) {
		this(objectClass, results, 
				Arrays.asList(objectClass.getAnnotation(GlueDataClass.class).defaultListColumns()));
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringWriter stringWriter = new StringWriter();
		Element docElem = getDocument().getDocumentElement();
		String objectType = XmlUtils.getXPathElement(docElem, OBJECT_TYPE).getTextContent();
		List<String> headers = XmlUtils.findChildElements(docElem, COLUMN).stream().
				map(Element::getTextContent).collect(Collectors.toList());
		List<Element> objElems = XmlUtils.findChildElements(docElem, OBJECT);
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
				List<Element> valueElems = XmlUtils.findChildElements(objElem);
				valueElems.forEach(valueElem -> {
					String cString;
					if(valueElem.getNodeName().equals(NULL)) {
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
		List<String> headers = XmlUtils.findChildElements(docElem, COLUMN).stream().
				map(Element::getTextContent).collect(Collectors.toList());
		int index = headers.indexOf(columnName)+1; // XPath indices start from 1
		List<Element> valueElems = XmlUtils.getXPathElements(docElem, OBJECT+"/*["+index+"]");
		return valueElems.stream().map(elem -> {
			if(elem.getNodeName().equals(NULL)) {
				return null;
			} else {
				return elem.getTextContent();
			}
		}).collect(Collectors.toList());
	}


	public List<Map<String, String>> asListOfMaps() {
		Element docElem = getDocument().getDocumentElement();
		List<String> headers = XmlUtils.findChildElements(docElem, COLUMN).stream().
				map(Element::getTextContent).collect(Collectors.toList());
		List<Element> objElems = XmlUtils.findChildElements(docElem, OBJECT);
		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		for(Element objElem: objElems) {
			Map<String, String> map = new LinkedHashMap<String, String>();
			List<Element> valueElems = XmlUtils.findChildElements(objElem);
			for(int i = 0; i < headers.size(); i++) {
				String header = headers.get(i);
				Element valueElem = valueElems.get(i);
				if(valueElem.getNodeName().equals(NULL)) {
					map.put(header, null);
				} else {
					map.put(header, valueElem.getTextContent());
				}
			}
			results.add(map);
		}
		return results;
	}

}
