package uk.ac.gla.cvr.gluetools.core.command;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public abstract class CommandUtils {

	public static <C extends GlueDataObject> ListCommandResult<C> runListCommand(
			CommandContext cmdContext, Class<C> theClass, SelectQuery query, List<String> fields) {
		ObjectContext objContext = cmdContext.getObjectContext();
		List<?> queryResult = objContext.performQuery(query);
		List<C> resultDataObjects = queryResult.stream().map(obj -> theClass.cast(obj)).collect(Collectors.toList());
		if(fields == null) {
			return new ListCommandResult<C>(theClass, resultDataObjects);
		} else {
			return new ListCommandResult<C>(theClass, resultDataObjects, fields.toArray(new String[]{}));
		}
	}

	public static <C extends GlueDataObject> ListCommandResult<C> runListCommand(
			CommandContext cmdContext, Class<C> theClass, SelectQuery query) {
		return runListCommand(cmdContext, theClass, query, null);
	}
}
