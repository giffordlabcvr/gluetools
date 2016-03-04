package uk.ac.gla.cvr.gluetools.core.command.result;

import java.util.List;
import java.util.Map;

public class TableResult extends BaseTableResult<Map<String, Object>> {

	public TableResult(String rootObjectName, List<String> columnHeaders, List<Map<String, Object>> rowData) {
		super(rootObjectName, rowData, buildColumns(columnHeaders));
	}

	
	@SuppressWarnings("unchecked")
	private static TableColumn<Map<String, Object>>[] buildColumns(List<String> columnHeaders) {
		TableColumn<Map<String, Object>>[] columns = new TableColumn[columnHeaders.size()];
		for(int i = 0; i < columnHeaders.size(); i++) {
			String header = columnHeaders.get(i);
			columns[i] = column(header, m -> m.get(header));
		}
		return columns;
	}


}
