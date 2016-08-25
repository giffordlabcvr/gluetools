package uk.ac.gla.cvr.gluetools.core.command.result;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ArrayReader;
import uk.ac.gla.cvr.gluetools.core.document.DocumentBuilder;
import uk.ac.gla.cvr.gluetools.core.document.DocumentReader;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;
import uk.ac.gla.cvr.gluetools.utils.RenderUtils;

import com.brsanthu.dataexporter.model.AlignType;
import com.brsanthu.dataexporter.model.Row;
import com.brsanthu.dataexporter.model.StringColumn;
import com.brsanthu.dataexporter.output.texttable.TextTableExportOptions;
import com.brsanthu.dataexporter.output.texttable.TextTableExportStyle;
import com.brsanthu.dataexporter.output.texttable.TextTableExporter;

public class BaseTableResult<D> extends CommandResult {

	public static final String VALUE = "value";
	public static final String ROW = "row";
	public static final String COLUMN = "column";

	
	@SafeVarargs
	public BaseTableResult(String rootObjectName, List<D> rowObjects, TableColumn<D> ... tableColumns) {
		super(rootObjectName);
		DocumentBuilder builder = getDocumentBuilder();
		ArrayBuilder columnsArrayBuilder = builder.setArray(COLUMN);
		for(TableColumn<D> column: tableColumns) {
			columnsArrayBuilder.add(column.getColumnHeader());
		}
		ArrayBuilder objectArrayBuilder = builder.setArray(ROW);
		for(D rowObject: rowObjects) {
			ArrayBuilder valueArrayBuilder = objectArrayBuilder.addObject().setArray(VALUE);
			for(TableColumn<D> column: tableColumns) {
				valueArrayBuilder.add(column.populateColumn(rowObject));
			}
		}

	}
	
	public static <D> TableColumn<D> column(String header, Function<D, Object> columnPopulator) {
		return new TableColumn<D>(header, columnPopulator);
	}

	
	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		List<String> columnHeaders = getColumnHeaders();
		List<Map<String, Object>> listOfMaps = asListOfMaps(columnHeaders);
		ArrayList<TablePage> tablePages = renderToTablePages(columnHeaders, listOfMaps, renderCtx);
		if(tablePages.size() == 0) {
			renderCtx.output("Empty result table");
		} else if(tablePages.size() == 1) {
			renderCtx.output(tablePages.get(0).content);
		} else {
			interactiveTableRender(renderCtx, "Row", tablePages);
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
	
	static class TablePage {
		String content;
		int numContentLines;
		int rowStart;
		int rowEnd;
		int totalRows;
	}
	
	private static class MyStringWriter extends StringWriter {
		int numLines = 0;
		int prevNumLines = 0;
		
		StringBuffer buf = new StringBuffer();
		public void commit() {
			super.write(buf.toString());
			buf = new StringBuffer();
			prevNumLines = numLines;
		}
		
		@Override
		public void write(int c) {
			buf.append(c);
			if(c == '\n') {
				numLines++;
			}
		}
		@Override
		public void write(char[] cbuf, int off, int len) {
			buf.append(cbuf, off, len);
			for(int i = off; i < off+len; i++) {
				if(cbuf[i] == '\n') {
					numLines++;
				}
			}
		}
		@Override
		public void write(String str) {
			buf.append(str);
			for(int i = 0; i < str.length(); i++) {
				if(str.charAt(i) == '\n') {
					numLines++;
				}
			}
		}
		@Override
		public void write(String str, int off, int len) {
			buf.append(str, off, len);
			for(int i = off; i < off+len; i++) {
				if(str.charAt(i) == '\n') {
					numLines++;
				}
			}
		}

		public void rollback() {
			buf = new StringBuffer();
			numLines = prevNumLines;
		}
	}

	public static ArrayList<TablePage> renderToTablePages(List<String> headers, List<Map<String, Object>> listOfMaps, 
			CommandResultRenderingContext renderCtx) {
		int numFound = listOfMaps.size();
		ArrayList<TablePage> tablePages = new ArrayList<TablePage>();

		ArrayList<Map<String,String>> renderedRows = renderAll(renderCtx, listOfMaps);
		
		Set<String> columnsWhichAllowSpaces = new LinkedHashSet<String>(headers);
		Map<String, Integer> widths = establishWidths(headers, renderCtx, renderedRows, columnsWhichAllowSpaces);
		
		if(numFound > 0) {
			int rowNumber = 1;
			while(rowNumber <= renderedRows.size()) {

				TablePage tablePage = new TablePage();
				tablePage.rowStart = rowNumber;
				tablePage.totalRows = renderedRows.size();
				MyStringWriter myStringWriter = new MyStringWriter();
				TextTableExportOptions options = new TextTableExportOptions();
				options.setStyle(TextTableExportStyle.CLASSIC);
				TextTableExporter textTable = new TextTableExporter(myStringWriter);
				for(String header: headers) {
					StringColumn column = new StringColumn(header, widths.get(header));
					column.setAlign(AlignType.TOP_LEFT);
					textTable.addColumns(column);
					myStringWriter.commit();
				}

				int maxLines;
				if(renderCtx.interactiveTables()) {
					maxLines = renderCtx.getTerminalHeight()-2;
				} else {
					maxLines = Integer.MAX_VALUE;
				}
				while( myStringWriter.numLines <= maxLines 
						&& rowNumber <= renderedRows.size()) {
					Map<String, String> headerToValue = renderedRows.get(rowNumber-1);
					Row row = new Row();
					headers.forEach(header -> {
						if(columnsWhichAllowSpaces.contains(header)) {
							row.addCellValue(" "+headerToValue.get(header));
						} else {
							row.addCellValue(headerToValue.get(header));
						}
					});
					textTable.addRows(row);
					if(myStringWriter.numLines <= maxLines) {
						myStringWriter.commit();
						tablePage.rowEnd = rowNumber;
						rowNumber++;
					} else {
						myStringWriter.rollback();
						break;
					}
				}
				textTable.finishExporting();
				myStringWriter.commit();
				tablePage.content = myStringWriter.toString();
				tablePage.numContentLines = myStringWriter.numLines;
				tablePages.add(tablePage);
			}
		}
		return tablePages;
	}

	private static Map<String, Integer> establishWidths(List<String> headers,
			CommandResultRenderingContext renderCtx,
			List<Map<String, String>> renderedRows,
			Set<String> columnsWhichAllowSpaces) {
		Map<String, Integer> widths = establishPreferredWidths(headers, renderedRows);
		int totalWidth = widths.values().stream().reduce(new Integer(0), Integer::sum) +
				headers.size()+1;
		// reduce column widths by decrementing the widest column until they fit.
		while(totalWidth > renderCtx.getTerminalWidth()) {
			Map.Entry<String,Integer> widestHeader = 
					widths.entrySet().stream().reduce(null, 
							(entry1, entry2) -> { 
								if(entry1 == null) { 
									return entry2;
								}
								if(entry1.getValue() < entry2.getValue()) { 
									return entry2; 
								} else { 
									return entry1; 
								} });
			widths.put(widestHeader.getKey(), widestHeader.getValue()-1);
			columnsWhichAllowSpaces.remove(widestHeader.getKey());
			totalWidth--;
		}
		return widths;
	}

	private static ArrayList<Map<String, String>> renderAll(CommandResultRenderingContext renderCtx, List<Map<String, Object>> listOfMaps) {
		return listOfMaps.stream()
			.map(unrendered -> {
				Map<String, String> rendered = new LinkedHashMap<String, String>();
				unrendered.forEach((k,v) -> {rendered.put(k, RenderUtils.render(v, renderCtx));});
				return rendered; })
			.collect(Collectors.toCollection(() -> new ArrayList<Map<String, String>>(listOfMaps.size())));
	}

	private static Map<String, Integer> establishPreferredWidths(List<String> headers, List<Map<String, String>> renderedRows) {
		Map<String, Integer> preferredWidths = new LinkedHashMap<String, Integer>();
		headers.forEach(header -> preferredWidths.merge(header, header.length()+2, Math::max));
		renderedRows.forEach(row -> row.forEach((header,value) -> preferredWidths.merge(header, value.length()+2, Math::max)));
		return preferredWidths;
	}

	public void updatePreferred(Map<String, Integer> preferredWidths, String header, String content) {
	}
	

	public static <D> List<Map<String, Object>> listOfMapsFromDataObjects(
			List<D> results, List<String> headers, 
			BiFunction<D, String, Object> resolveHeaderFunction) {
		List<Map<String, Object>> listOfMaps = new ArrayList<Map<String, Object>>();
		for(D object: results) {
			listOfMaps.add(MapResult.mapFromDataObject(headers, object, resolveHeaderFunction));
		}
		return listOfMaps;
	}

	@Override
	protected void renderToConsoleAsTab(CommandResultRenderingContext renderCtx) {
		renderDelimitedTable(renderCtx, "\t");
	}
	@Override
	protected void renderToConsoleAsCsv(CommandResultRenderingContext renderCtx) {
		renderDelimitedTable(renderCtx, ",");
	}

	private void renderDelimitedTable(CommandResultRenderingContext renderCtx,
			String delimiter) {
		List<String> columnHeaders = getColumnHeaders();
		List<Map<String, Object>> listOfMaps = asListOfMaps(columnHeaders);
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < columnHeaders.size(); i++) {
			buf.append(columnHeaders.get(i));
			if(i < columnHeaders.size() - 1) {
				buf.append(delimiter);
			}
		}
		renderCtx.output(buf.toString());
		for(Map<String, Object> rowData: listOfMaps) {
			buf = new StringBuffer();
			for(int i = 0; i < columnHeaders.size(); i++) {
				String header = columnHeaders.get(i);
				buf.append(RenderUtils.render(rowData.get(header), renderCtx));
				if(i < columnHeaders.size() - 1) {
					buf.append(delimiter);
				}
			}
			renderCtx.output(buf.toString());
		}
	}

	public void interactiveTableRender(CommandResultRenderingContext renderCtx, String objectType,
			ArrayList<TablePage> tablePages) {
		int pageNumber = 0;
		boolean finished = false;
		while(!finished) {
			TablePage tablePage = tablePages.get(pageNumber);
			StringBuffer buf = new StringBuffer(tablePage.content);
			int numLines = tablePage.numContentLines;
			while(numLines < renderCtx.getTerminalHeight()-1) {
				buf.append("\n");
				numLines++;
			}
			buf.append(objectType)
			.append("s ")
			.append(tablePage.rowStart)
			.append(" to ")
			.append(tablePage.rowEnd)
			.append(" of ")
			.append(tablePage.totalRows)
			.append(" [F:first, L:last, P:prev, N:next, Q:quit]");
			renderCtx.output(buf.toString(), false);
			String nextAction = "";
			while(nextAction.equals("")) {
				try {
					int read = renderCtx.getInputStream().read();
					if(read >= 0) {
						nextAction = Character.toString((char) read).toUpperCase();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				if(nextAction.equals("F")) {
					pageNumber = 0;
				} else if(nextAction.equals("L")) {
					pageNumber = tablePages.size()-1;
				} else if(nextAction.equals("P")) {
					pageNumber = Math.max(pageNumber-1, 0);
				} else if(nextAction.equals("N")) {
					pageNumber = Math.min(pageNumber+1, tablePages.size()-1);
				} else if(nextAction.equals("Q")) {
					finished = true;
				} else {
					nextAction = "";
				}
			}
			renderCtx.output("");
		}
	}


}
