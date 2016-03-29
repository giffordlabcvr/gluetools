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
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptFSM.Node;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptParseResult;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptParseResult.OptionsDisplay;

public class AdvancedCmdCompleter extends CommandCompleter {

	public abstract static class VariableInstantiator {
		@SuppressWarnings("rawtypes")
		protected abstract List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, Map<String, Object> bindings, String prefix);
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
		@SuppressWarnings("rawtypes")
		protected List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			return listNames(cmdContext, prefix, theClass, nameProperty);
		}
	}

	private class StaticStringListInstantiator extends VariableInstantiator {
		private List<String> staticStringList;
		@SuppressWarnings("unused")
		public StaticStringListInstantiator(List<String> staticStringList) {
			super();
			this.staticStringList = staticStringList;
		}
		@Override
		@SuppressWarnings("rawtypes")
		protected List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			return staticStringList.stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
		}
	}

	
	protected abstract class QualifiedDataObjectNameInstantiator extends VariableInstantiator {
		private Class<? extends GlueDataObject> theClass;
		private String nameProperty;
		@SuppressWarnings("unused")
		public QualifiedDataObjectNameInstantiator(
				Class<? extends GlueDataObject> theClass, String nameProperty) {
			super();
			this.theClass = theClass;
			this.nameProperty = nameProperty;
		}
		@Override
		@SuppressWarnings("rawtypes")
		protected final List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			Map<String, Object> qualifierValues = new LinkedHashMap<String, Object>();
			qualifyResults(cmdContext.peekCommandMode(), bindings, qualifierValues);
			Expression matchExp = ExpressionFactory.expTrue();
			for(Map.Entry<String, Object> qualifierEntry: qualifierValues.entrySet()) {
				matchExp = matchExp.andExp(ExpressionFactory.matchExp(qualifierEntry.getKey(), qualifierEntry.getValue()));
			}
			return listNames(cmdContext, prefix, theClass, nameProperty, matchExp);
		}

		@SuppressWarnings("rawtypes")
		protected abstract void qualifyResults(CommandMode cmdMode, Map<String, Object> bindings, Map<String, Object> qualifierValues);
	}

	
	
	private class SimpleEnumInstantiator extends VariableInstantiator {
		private Class<? extends Enum<?>> theClass;
		public SimpleEnumInstantiator(Class<? extends Enum<?>> theClass) {
			super();
			this.theClass = theClass;
		}
		@Override
		@SuppressWarnings("rawtypes")
		protected List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
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
		@SuppressWarnings("rawtypes")
		protected List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			return completePath(cmdContext, prefix, directoriesOnly);
		}
	}
	
	
	private Map<String, VariableInstantiator> variableInstantiators = new LinkedHashMap<String, VariableInstantiator>();
	
	protected <A extends VariableInstantiator> A registerVariableInstantiator(String variableName, A variableInstantiator) {
		variableInstantiators.put(variableName, variableInstantiator);
		return variableInstantiator;
	}

	protected void registerStringListLookup(String variableName, List<String> staticStringList) {
		registerVariableInstantiator(variableName, new StaticStringListInstantiator(staticStringList));
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
		String optionsDisplayString = cmdContext.getOptionValue(ConsoleOption.COMPLETER_OPTIONS_DISPLAY);
		DocoptParseResult.OptionsDisplay optionsDisplay = DocoptParseResult.OptionsDisplay.valueOf(optionsDisplayString.toUpperCase());
		if(optionsDisplay == OptionsDisplay.SHORT_ONLY && prefix.startsWith("--")) {
			optionsDisplay = OptionsDisplay.BOTH;
		}
		if(optionsDisplay == OptionsDisplay.LONG_ONLY && prefix.matches("^-[a-zA-Z]$")) {
			optionsDisplay = OptionsDisplay.BOTH;
		}
		DocoptParseResult parseResult = DocoptParseResult.parse(argStrings, optionsMap, startNode, optionsDisplay);
		String variableName = parseResult.getNextVariable();
		List<CompletionSuggestion> results = new ArrayList<CompletionSuggestion>();
		if(variableName != null) {
			Map<String, Object> bindings = parseResult.getBindings();
			List<CompletionSuggestion> instantiations = null;
			VariableInstantiator variableInstantiator = variableInstantiators.get(variableName);
			if(variableInstantiator != null) {
				instantiations = variableInstantiator.instantiate(cmdContext, cmdClass, bindings, prefix);
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

	public static final List<CompletionSuggestion> listNames(
			ConsoleCommandContext cmdContext, String prefix, Class<? extends GlueDataObject> theClass, String nameProperty) {
		return listNames(cmdContext, prefix, theClass, nameProperty, ExpressionFactory.expTrue());
	}

	private static final List<CompletionSuggestion> listEnumValues(
			ConsoleCommandContext cmdContext, String prefix, Class<? extends Enum<?>> theClass) {
		return Arrays.asList(theClass.getEnumConstants()).stream()
				.map(ec -> new CompletionSuggestion(ec.name(), true)).collect(Collectors.toList());
	}

	
	
	protected static final <A extends GlueDataObject> List<CompletionSuggestion> 
		listNames(ConsoleCommandContext cmdContext, String prefix, Class<A> theClass, 
			String nameProperty, Expression whereClause) {
		if(prefix != null && prefix.length()>0) {
			whereClause = whereClause.andExp(ExpressionFactory.likeExp(nameProperty, prefix+"%"));
		}
		List<A> queryResults = GlueDataObject.query(cmdContext, theClass, new SelectQuery(theClass, whereClause));
		return queryResults
				.stream()
				.map(s -> new CompletionSuggestion(s.readNestedProperty(nameProperty).toString(), true))
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
	
	
	private static abstract class FieldNameInstantiator extends VariableInstantiator {
		
		private ConfigurableTable cTable;

		protected FieldNameInstantiator(ConfigurableTable cTable) {
			this.cTable = cTable;
		}

		@Override
		@SuppressWarnings("rawtypes")
		protected List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			return getFieldNames(cmdContext).stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
		}

		protected ConfigurableTable getCTable() {
			return cTable;
		}
		
		protected abstract List<String> getFieldNames(ConsoleCommandContext cmdContext);

		protected Project getProject(ConsoleCommandContext cmdContext) {
			InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
			Project project = insideProjectMode.getProject();
			return project;
		}
	}

	public static final class CustomFieldNameInstantiator extends FieldNameInstantiator {
		public CustomFieldNameInstantiator(ConfigurableTable cTable) {
			super(cTable);
		}
		@Override
		protected List<String> getFieldNames(ConsoleCommandContext cmdContext) {
			return getProject(cmdContext).getCustomFieldNames(getCTable());
		}
	}

	public static final class ModifiableFieldNameInstantiator extends FieldNameInstantiator {
		public ModifiableFieldNameInstantiator(ConfigurableTable cTable) {
			super(cTable);
		}
		@Override
		protected List<String> getFieldNames(ConsoleCommandContext cmdContext) {
			return getProject(cmdContext).getModifiableFieldNames(getCTable());
		}
	}

	public static final class ListablePropertyInstantiator extends FieldNameInstantiator {
		public ListablePropertyInstantiator(ConfigurableTable cTable) {
			super(cTable);
		}
		@Override
		protected List<String> getFieldNames(ConsoleCommandContext cmdContext) {
			return getProject(cmdContext).getListableProperties(getCTable());
		}
	}


}
