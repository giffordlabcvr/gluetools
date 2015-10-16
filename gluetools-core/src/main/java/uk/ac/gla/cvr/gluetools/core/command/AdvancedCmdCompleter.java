package uk.ac.gla.cvr.gluetools.core.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptFSM.Node;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptParseResult;

public class AdvancedCmdCompleter extends CommandCompleter {

	protected abstract class VariableInstantiator {
		protected abstract List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext, Map<String, Object> bindings, String prefix);
	}
	
	private class SimpleDataObjectNameInstantiator extends VariableInstantiator {
		private Class<? extends GlueDataObject> theClass;
		private String nameProperty;
		@SuppressWarnings("unused")
		public SimpleDataObjectNameInstantiator(
				Class<? extends GlueDataObject> theClass, String nameProperty) {
			super();
			this.theClass = theClass;
			this.nameProperty = nameProperty;
		}
		@Override
		protected List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Map<String, Object> bindings,
				String prefix) {
			return listNames(cmdContext, prefix, theClass, nameProperty);
		}
	}

	private class SimpleEnumInstantiator extends VariableInstantiator {
		private Class<? extends Enum<?>> theClass;
		public SimpleEnumInstantiator(Class<? extends Enum<?>> theClass) {
			super();
			this.theClass = theClass;
		}
		@Override
		protected List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Map<String, Object> bindings,
				String prefix) {
			return listEnumValues(cmdContext, prefix, theClass);
		}
	}

	
	private class SimplePathInstantiator extends VariableInstantiator {
		private boolean directoriesOnly;
		
		public SimplePathInstantiator(boolean directoriesOnly) {
			super();
			this.directoriesOnly = directoriesOnly;
		}

		@Override
		protected List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Map<String, Object> bindings,
				String prefix) {
			return completePath(cmdContext, prefix, directoriesOnly);
		}
	}
	
	
	private Map<String, VariableInstantiator> variableInstantiators = new LinkedHashMap<String, VariableInstantiator>();
	
	protected void registerVariableInstantiator(String variableName, VariableInstantiator variableInstantiator) {
		variableInstantiators.put(variableName, variableInstantiator);
	}

	protected void registerDataObjectNameLookup(String variableName, Class<? extends GlueDataObject> theClass, String nameProperty) {
		registerVariableInstantiator(variableName, new SimpleDataObjectNameInstantiator(theClass, nameProperty));
	}

	protected void registerEnumLookup(String variableName, Class<? extends Enum<?>> theClass) {
		registerVariableInstantiator(variableName, new SimpleEnumInstantiator(theClass));
	}

	protected void registerPathLookup(String variableName, boolean directoriesOnly) {
		registerVariableInstantiator(variableName, new SimplePathInstantiator(directoriesOnly));
	}

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
			List<CompletionSuggestion> instantiations = null;
			VariableInstantiator variableInstantiator = variableInstantiators.get(variableName);
			if(variableInstantiator != null) {
				instantiations = variableInstantiator.instantiate(cmdContext, bindings, prefix);
			}
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
			} else {
				results.add(new CompletionSuggestion("<"+variableName+">", true));
			}
		}
		results.addAll(
				parseResult.getNextLiterals().stream()
				.map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList()));
		return results;

	}

	private static final List<CompletionSuggestion> listNames(
			ConsoleCommandContext cmdContext, String prefix, Class<? extends GlueDataObject> theClass, String nameProperty) {
		return listNames(cmdContext, prefix, theClass, nameProperty, ExpressionFactory.expTrue());
	}

	private static final List<CompletionSuggestion> listEnumValues(
			ConsoleCommandContext cmdContext, String prefix, Class<? extends Enum<?>> theClass) {
		return Arrays.asList(theClass.getEnumConstants()).stream()
				.map(ec -> new CompletionSuggestion(ec.name(), true)).collect(Collectors.toList());
	}

	
	
	protected static final List<CompletionSuggestion> listNames(ConsoleCommandContext cmdContext, String prefix, Class<? extends GlueDataObject> theClass, 
			String nameProperty, Expression whereClause) {
		if(prefix != null && prefix.length()>0) {
			whereClause = whereClause.andExp(ExpressionFactory.likeExp(nameProperty, prefix+"%"));
		}
		ListResult listCmdResult = CommandUtils.runListCommand(cmdContext, theClass, new SelectQuery(theClass, whereClause));
		return listCmdResult.getColumnValues(nameProperty)
				.stream()
				.map(s -> new CompletionSuggestion(s, true))
				.collect(Collectors.toList());
	}

	private static List<CompletionSuggestion> completePath(ConsoleCommandContext cmdContext, String prefix, boolean directoriesOnly) {
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
			List<String> fileMembers = cmdContext.listMembers(parentPath, !directoriesOnly, false, searchPrefix);
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
