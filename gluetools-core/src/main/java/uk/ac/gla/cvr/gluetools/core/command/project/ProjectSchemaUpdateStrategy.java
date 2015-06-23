package uk.ac.gla.cvr.gluetools.core.command.project;

import java.sql.SQLException;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.merge.MergerToken;

public class ProjectSchemaUpdateStrategy extends CreateIfNoSchemaStrategy {

	 @Override
	    protected void processSchemaUpdate(DataNode dataNode) throws SQLException {
		 
		 MergerToken mergerTok;
	 }
}
