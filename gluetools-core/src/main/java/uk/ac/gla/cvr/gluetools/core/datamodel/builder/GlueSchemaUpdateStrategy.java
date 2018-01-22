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
