package uk.ac.gla.cvr.gluetools.core.command;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public abstract class CommandUtils {

	public static <C extends GlueDataObject> ListCommandResult<C> runListCommand(
			CommandContext cmdContext, Class<C> theClass, SelectQuery query) {
		ObjectContext objContext = cmdContext.getObjectContext();
		List<?> queryResult = objContext.performQuery(query);
		List<C> projects = queryResult.stream().map(obj -> theClass.cast(obj)).collect(Collectors.toList());
		return new ListCommandResult<C>(theClass, projects);
	}
	
}
