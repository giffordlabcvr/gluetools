package uk.ac.gla.cvr.gluetools.core.command.result;

import java.util.function.Function;

public class TableColumn<S> {

	private String columnHeader;
	private Function<S, Object> columnPopulator;
	
	public TableColumn(String columnHeader, Function<S, Object> columnPopulator) {
		super();
		this.columnHeader = columnHeader;
		this.columnPopulator = columnPopulator;
	}

	public String getColumnHeader() {
		return columnHeader;
	}

	public Object populateColumn(S object) {
		return columnPopulator.apply(object);
	}
	
	
	
}
