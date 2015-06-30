package uk.ac.gla.cvr.gluetools.core.datamodel.builder;

import java.sql.SQLException;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.datamodel.meta.SchemaVersion;

public class GlueSchemaUpdateStrategy extends CreateIfNoSchemaStrategy {

	@Override
	protected void processSchemaUpdate(DataNode dataNode) throws SQLException {
		String dbSchemaVersion = GluetoolsEngine.getInstance().getDbSchemaVersion();
		if(dbSchemaVersion.equals("0") || dbSchemaVersion.equals(SchemaVersion.currentVersionString)) {
			super.processSchemaUpdate(dataNode);
		} else {
			// here we would add specific actions to update from specific versions.

			// should not happen.
			throw new RuntimeException("Unable to migrate database schema version from "+dbSchemaVersion+" to version "+SchemaVersion.currentVersionString);
		}
	}

	public static boolean canMigrateFromSchemaVersion(String dbSchemaVersion) {
		if(dbSchemaVersion.equals("0") || dbSchemaVersion.equals(SchemaVersion.currentVersionString)) {
			return true;
		} else {
			return false;
		}
	}

	
}
