package uk.ac.gla.cvr.gluetools.core.command;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptFSM.Node;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptParseResult;

public class AdvancedCmdCompleter extends CommandCompleter {

	@SuppressWarnings("unchecked")
	@Override
	public final List<String> completionSuggestions(ConsoleCommandContext cmdContext,
			@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, List<String> argStrings) {

		CommandUsage cmdUsage = CommandUsage.commandUsageForCmdClass(cmdClass);
		Map<Character, String> optionsMap = cmdUsage.optionsMap();
		Node startNode = cmdUsage.createFSM(optionsMap);
		DocoptParseResult parseResult = DocoptParseResult.parse(argStrings, optionsMap, startNode);
		String variableName = parseResult.getNextVariable();
		if(variableName != null) {
			Map<String, Object> bindings = parseResult.getBindings();
			List<String> instantiations = instantiateVariable(cmdContext, bindings, variableName);
			if(instantiations != null) {
				Object currentBinding = bindings.get(variableName);
				if(currentBinding != null && currentBinding instanceof String) {
					instantiations.remove((String) currentBinding);
				}
				if(currentBinding != null && currentBinding instanceof List<?>) {
					instantiations.removeAll((List<String>) currentBinding);
				}
				return instantiations;
			}
			return Collections.emptyList();
		}
		return parseResult.getNextLiterals();

	}

	protected List<String> instantiateVariable(ConsoleCommandContext cmdContext, Map<String, Object> bindings, String variableName) {
		return null;
	}
	
	protected final List<String> listNames(ConsoleCommandContext cmdContext, Class<? extends GlueDataObject> theClass,
			String nameProperty) {
		ListResult listCmdResult = CommandUtils.runListCommand(cmdContext, theClass, new SelectQuery(theClass));
		return listCmdResult.getColumnValues(nameProperty);
	}

	protected final List<String> listNames(ConsoleCommandContext cmdContext, Class<? extends GlueDataObject> theClass, 
			String nameProperty, Expression whereClause) {
		ListResult listCmdResult = CommandUtils.runListCommand(cmdContext, theClass, new SelectQuery(theClass, whereClause));
		return listCmdResult.getColumnValues(nameProperty);
	}

}
