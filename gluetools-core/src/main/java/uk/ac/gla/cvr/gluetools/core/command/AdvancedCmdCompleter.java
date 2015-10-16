package uk.ac.gla.cvr.gluetools.core.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
	public final List<CompletionSuggestion> completionSuggestions(ConsoleCommandContext cmdContext,
			@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, List<String> argStrings, String prefix) {

		CommandUsage cmdUsage = CommandUsage.commandUsageForCmdClass(cmdClass);
		Map<Character, String> optionsMap = cmdUsage.optionsMap();
		Node startNode = cmdUsage.createFSM(optionsMap);
		DocoptParseResult parseResult = DocoptParseResult.parse(argStrings, optionsMap, startNode);
		String variableName = parseResult.getNextVariable();
		List<CompletionSuggestion> results = new ArrayList<CompletionSuggestion>();
		if(variableName != null) {
			Map<String, Object> bindings = parseResult.getBindings();
			List<CompletionSuggestion> instantiations = instantiateVariable(cmdContext, bindings, prefix, variableName);
			if(instantiations != null) {
				Object currentBinding = bindings.get(variableName);
				if(currentBinding != null && currentBinding instanceof String) {
					instantiations = instantiations.stream()
							.filter(i -> !i.getSuggestedWord().equals((String) currentBinding))
							.collect(Collectors.toList());
				}
				if(currentBinding != null && currentBinding instanceof List<?>) {
					instantiations = instantiations.stream()
							.filter(i -> !((List<String>) currentBinding).contains(i.getSuggestedWord()))
							.collect(Collectors.toList());
				}
				results.addAll(instantiations);
			}
		}
		results.addAll(
				parseResult.getNextLiterals().stream()
				.map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList()));
		return results;

	}

	protected List<CompletionSuggestion> instantiateVariable(ConsoleCommandContext cmdContext, Map<String, Object> bindings, String prefix, String variableName) {
		return null;
	}
	
	protected final List<CompletionSuggestion> listNames(ConsoleCommandContext cmdContext, Class<? extends GlueDataObject> theClass,
			String nameProperty) {
		ListResult listCmdResult = CommandUtils.runListCommand(cmdContext, theClass, new SelectQuery(theClass));
		return listCmdResult.getColumnValues(nameProperty)
				.stream()
				.map(s -> new CompletionSuggestion(s, true))
				.collect(Collectors.toList());
	}

	protected final List<CompletionSuggestion> listNames(ConsoleCommandContext cmdContext, Class<? extends GlueDataObject> theClass, 
			String nameProperty, Expression whereClause) {
		ListResult listCmdResult = CommandUtils.runListCommand(cmdContext, theClass, new SelectQuery(theClass, whereClause));
		return listCmdResult.getColumnValues(nameProperty)
				.stream()
				.map(s -> new CompletionSuggestion(s, true))
				.collect(Collectors.toList());
	}

	public List<CompletionSuggestion> completePath(ConsoleCommandContext cmdContext, String prefix) {
		// parent path might be
		// the configured load-save-path if prefix does not have any path.
		// an absolute path specified by the pathFromPrefix
		// the configured load-save-path plus a relative path specified by pathFromPrefix.
		File parentPath = new File(cmdContext.getOptionValue(ConsoleOption.LOAD_SAVE_PATH));
		File prefixPath = new File(prefix);
		String searchPrefix;
		
		File pathFromPrefix = null;
		if(prefix.endsWith("/")) {
			pathFromPrefix = new File(prefix.substring(0, prefix.length()-1));
			searchPrefix = "";
		} else {
			pathFromPrefix = prefixPath.getParentFile();
			searchPrefix = prefixPath.getName();
		}
		if(pathFromPrefix != null) {
			if(pathFromPrefix.isAbsolute()) {
				parentPath = pathFromPrefix;
			} else {
				parentPath = new File(parentPath, pathFromPrefix.toString());
			}
		}
		List<String> stringResults = new ArrayList<String>();
		if(cmdContext.isDirectory(parentPath.toString())) {
			List<String> fileMembers = cmdContext.listMembers(parentPath, true, false, searchPrefix);
			List<String> directoryMembers = cmdContext
					.listMembers(parentPath, false, true, searchPrefix)
					.stream()
					.map(s -> s+"/")
					.collect(Collectors.toList());
			List<String> allMembers = new ArrayList<String>(fileMembers);
			allMembers.addAll(directoryMembers);
			if(pathFromPrefix != null) {
				File prefixParentPath0 = pathFromPrefix;
				stringResults.addAll(allMembers
						.stream()
						.map(s -> prefixParentPath0.toString()+"/"+s)
						.collect(Collectors.toList()));
			} else {
				stringResults.addAll(allMembers);
			}
		}
		List<CompletionSuggestion> suggestions = stringResults.stream()
				.map(s -> {
					if(s.endsWith("/")) {
						return new CompletionSuggestion(s, false);
					} else {
						return new CompletionSuggestion(s, true);
					}
				}).collect(Collectors.toList());
		return suggestions;
	}

}
