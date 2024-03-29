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

import org.apache.commons.lang3.StringEscapeUtils;

import com.brsanthu.dataexporter.model.AlignType;
import com.brsanthu.dataexporter.model.Row;
import com.brsanthu.dataexporter.model.StringColumn;
import com.brsanthu.dataexporter.output.texttable.TextTableExportOptions;
import com.brsanthu.dataexporter.output.texttable.TextTableExportStyle;
import com.brsanthu.dataexporter.output.texttable.TextTableExporter;

import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.utils.RenderUtils;

public class BaseTableResult<D> extends CommandResult {

	public static final String VALUE = "value";
	public static final String ROW = "row";
	public static final String COLUMN = "column";

	private int numRows;
	
	@SafeVarargs
	public BaseTableResult(String rootObjectName, List<D> rowObjects, TableColumn<D> ... tableColumns) {
		super(rootObjectName);
		this.numRows = rowObjects.size();
		CommandDocument builder = getCommandDocument();
		CommandArray columnsArrayBuilder = builder.setArray(COLUMN);
		for(TableColumn<D> column: tableColumns) {
			columnsArrayBuilder.add(column.getColumnHeader());
		}
		CommandArray objectArrayBuilder = builder.setArray(ROW);
		for(D rowObject: rowObjects) {
			CommandArray valueArrayBuilder = objectArrayBuilder.addObject().setArray(VALUE);
			for(TableColumn<D> column: tableColumns) {
				valueArrayBuilder.add(column.populateColumn(rowObject));
			}
		}

	}
	
	public int getNumRows() {
		return numRows;
	}
	
	public static <D> TableColumn<D> column(String header, Function<D, Object> columnPopulator) {
		return new TableColumn<D>(header, columnPopulator);
	}

	
	@Override
	protected void renderToConsoleAsText(InteractiveCommandResultRenderingContext renderCtx) {
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
		CommandDocument commandDocument = getCommandDocument();
		CommandArray rowsArray = commandDocument.getArray(ROW);
		for(int i = 0; i < rowsArray.size(); i++) {
			CommandObject rowObject = rowsArray.getObject(i);
			CommandArray valuesArray = rowObject.getArray(VALUE);
			Object value = valuesArray.getSimpleValue(index);
			if(value == null) {
				values.add(null);
			} else {
				values.add(value.toString());
			}
		}
		return values;
	}
	
	


	public List<Map<String, Object>> asListOfMaps(List<String> headers) {
		CommandDocument commandDocument = getCommandDocument();
		CommandArray rowsArray = commandDocument.getArray(ROW);
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		for(int i = 0; i < rowsArray.size(); i++) {
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			CommandObject rowObject = rowsArray.getObject(i);
			CommandArray valuesArray = rowObject.getArray(VALUE);
			for(int j = 0; j < headers.size(); j++) {
				String header = headers.get(j);
				map.put(header, valuesArray.getSimpleValue(j));
			}
			results.add(map);
		}
		return results;
	}
	
	public List<Map<String, Object>> asListOfMaps() {
		return asListOfMaps(getColumnHeaders());
	}

	public List<String> getColumnHeaders() {
		CommandArray columnsArray = getCommandDocument().getArray(COLUMN);
		List<String> headers = new ArrayList<String>();
		for(int i = 0; i < columnsArray.size(); i++) {
			headers.add(columnsArray.getString(i));
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
			InteractiveCommandResultRenderingContext renderCtx) {
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
						String cellValue = headerToValue.get(header);
						if(columnsWhichAllowSpaces.contains(header)) {
							cellValue = " "+cellValue;
						} 
						int tableTruncationLimit = renderCtx.getTableTruncationLimit();
						if(cellValue.length() > tableTruncationLimit) {
							cellValue = cellValue.substring(0, tableTruncationLimit-3)+"...";
						}
						row.addCellValue(cellValue);
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
			InteractiveCommandResultRenderingContext renderCtx,
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
	protected void renderResultAsTab(CommandResultRenderingContext renderCtx) {
		renderDelimitedTable(renderCtx, "\t", null);
	}
	@Override
	protected void renderResultAsCsv(CommandResultRenderingContext renderCtx) {
		renderDelimitedTable(renderCtx, ",", new Function<String, String>() {
			@Override
			public String apply(String t) {
				return StringEscapeUtils.escapeCsv(t);
			}
		});
	}

	private void renderDelimitedTable(CommandResultRenderingContext renderCtx,
			String delimiter, Function<String,String> escapeFunction) {
		StringBuffer buf;
		List<String> columnHeaders = getColumnHeaders();
		List<Map<String, Object>> listOfMaps = asListOfMaps(columnHeaders);
		if(renderCtx.renderTableHeaders()) {
			buf = new StringBuffer();
			for(int i = 0; i < columnHeaders.size(); i++) {
				String columnHeader = columnHeaders.get(i);
				if(escapeFunction != null) {
					columnHeader = escapeFunction.apply(columnHeader);
				}
				buf.append(columnHeader);
				if(i < columnHeaders.size() - 1) {
					buf.append(delimiter);
				}
			}
			renderCtx.output(buf.toString());
		}
		for(Map<String, Object> rowData: listOfMaps) {
			buf = new StringBuffer();
			int numColumnsInRow = columnHeaders.size();
			if(renderCtx.trimNullValues()) {
				numColumnsInRow = 0;
				for(int i = 0; i < columnHeaders.size(); i++) {
					String header = columnHeaders.get(i);
					if(rowData.get(header) != null) {
						numColumnsInRow = i+1;
					}
				}
			}
			for(int i = 0; i < numColumnsInRow; i++) {
				String header = columnHeaders.get(i);
				String value = RenderUtils.render(rowData.get(header), renderCtx);
				if(escapeFunction != null) {
					value = escapeFunction.apply(value);
				}
				buf.append(value);
				if(i < numColumnsInRow - 1) {
					buf.append(delimiter);
				}
			}
			renderCtx.output(buf.toString());
		}
	}

	public void interactiveTableRender(InteractiveCommandResultRenderingContext renderCtx, String objectType,
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
