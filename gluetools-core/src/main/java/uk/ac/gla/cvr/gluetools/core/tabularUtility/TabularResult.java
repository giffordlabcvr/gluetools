package uk.ac.gla.cvr.gluetools.core.tabularUtility;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.tabularUtility.TabularUtility.TabularData;

public class TabularResult extends BaseTableResult<String[]> {

	public TabularResult(TabularData tabularData) {
		super("tabularResult", tabularData.getRows(), tableColumns(tabularData.getColumnNames()));
	}

	@SuppressWarnings("unchecked")
	private static TableColumn<String[]>[]  tableColumns(String[] columnNames) {
		TableColumn<String[]>[] columns = new TableColumn[columnNames.length];
		for(int i = 0; i < columnNames.length; i++) {
			columns[i] = new LoadTabularResultTableColumn(columnNames[i], i);
		}
		return columns;
	}
	
	private static class LoadTabularResultTableColumn extends TableColumn<String[]> {
		public LoadTabularResultTableColumn(String columnHeader, int columnIndex) {
			super(columnHeader, strArray -> strArray[columnIndex]);
		}
	}
}
