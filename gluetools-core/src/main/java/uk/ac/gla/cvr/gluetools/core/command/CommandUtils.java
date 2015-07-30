package uk.ac.gla.cvr.gluetools.core.command;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public abstract class CommandUtils {

	public static <C extends GlueDataObject> ListResult runListCommand(
			CommandContext cmdContext, Class<C> theClass, SelectQuery query, List<String> fields) {
		ObjectContext objContext = cmdContext.getObjectContext();
		List<C> resultDataObjects = GlueDataObject.query(objContext, theClass, query);
		return new ListResult(theClass, resultDataObjects, fields);
	}

	public static <C extends GlueDataObject> ListResult runListCommand(
			CommandContext cmdContext, Class<C> theClass, SelectQuery query) {
		return runListCommand(cmdContext, theClass, query, null);
	}

	
}
