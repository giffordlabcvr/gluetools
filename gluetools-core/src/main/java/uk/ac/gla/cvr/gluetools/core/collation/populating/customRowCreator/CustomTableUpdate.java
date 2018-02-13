package uk.ac.gla.cvr.gluetools.core.collation.populating.customRowCreator;

public class CustomTableUpdate {

	private boolean updated;
	private String tableName;
	private String newRowId;
	
	public CustomTableUpdate(boolean updated, String tableName, String newRowId) {
		super();
		this.updated = updated;
		this.tableName = tableName;
		this.newRowId = newRowId;
	}

	public boolean isUpdated() {
		return updated;
	}

	public String getTableName() {
		return tableName;
	}

	public String getNewRowId() {
		return newRowId;
	}
	
	
}
