package uk.ac.gla.cvr.gluetools.core.collation.populating.customRowCreator;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

public interface CustomRowCreator {

	public String tableName();
	
	public static CustomTableUpdate createCustomTableUpdate(CommandContext cmdContext, String tableName, String newRowId) {
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		CustomTable customTable = project.getCustomTable(tableName);
		// check row exists.
		CustomTableObject existingRow = GlueDataObject.lookup(cmdContext, customTable.getRowClass(), CustomTableObject.pkMap(newRowId), true);
		if(existingRow == null) {
			return new CustomTableUpdate(true, tableName, newRowId);
		} else {
			return new CustomTableUpdate(false, tableName, newRowId);
		}
	}
	
	public static void applyUpdateToDB(CommandContext cmdContext, CustomTableUpdate update) {
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		CustomTable customTable = project.getCustomTable(update.getTableName());
		GlueDataObject.create(cmdContext, customTable.getRowClass(), CustomTableObject.pkMap(update.getNewRowId()), false);

	}
}
