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
import uk.ac.gla.cvr.gluetools.core.command.project.ListModuleCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptFSM.Node;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptParseResult;
import uk.ac.gla.cvr.gluetools.core.docopt.DocoptParseResult.OptionsDisplay;

public class AdvancedCmdCompleter extends CommandCompleter {

	public abstract static class VariableInstantiator {
		private boolean allowsDuplicateListSuggestions = false;
		@SuppressWarnings("rawtypes")
		public abstract List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, Map<String, Object> bindings, String prefix);

		public boolean allowsDuplicateListSuggestions() {
			return allowsDuplicateListSuggestions;
		}

		public void setAllowsDuplicateListSuggestions(boolean allowsDuplicateListSuggestions) {
			this.allowsDuplicateListSuggestions = allowsDuplicateListSuggestions;
		}
		
		
	}
	
	public static class SimpleDataObjectNameInstantiator extends VariableInstantiator {
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
		public List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			return listNames(cmdContext, prefix, theClass, nameProperty);
		}
	}

	public static class StaticStringListInstantiator extends VariableInstantiator {
		private List<String> staticStringList;
		@SuppressWarnings("unused")
		public StaticStringListInstantiator(List<String> staticStringList) {
			super();
			this.staticStringList = staticStringList;
		}
		@Override
		@SuppressWarnings("rawtypes")
		public List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
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
		public
		final List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
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

	
	
	public static class SimpleEnumInstantiator extends VariableInstantiator {
		private Class<? extends Enum<?>> theClass;
		public SimpleEnumInstantiator(Class<? extends Enum<?>> theClass) {
			super();
			this.theClass = theClass;
		}
		@Override
		@SuppressWarnings("rawtypes")
		public List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			return listEnumValues(cmdContext, prefix, theClass);
		}
	}

	
	public static class SimplePathInstantiator extends VariableInstantiator {
		private boolean directoriesOnly;
		
		public SimplePathInstantiator(boolean directoriesOnly) {
			super();
			this.directoriesOnly = directoriesOnly;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public List<CompletionSuggestion> instantiate(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
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
	
	protected void registerModuleNameLookup(String variableName, String moduleType) {
		registerVariableInstantiator(variableName, new VariableInstantiator() {
			@Override
			public List<CompletionSuggestion> instantiate(
					ConsoleCommandContext cmdContext,
					@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
					String prefix) {
				List<Module> modules = ListModuleCommand.listModules(cmdContext, moduleType);
				return modules.stream().map(m -> new CompletionSuggestion(m.getName(), true)).collect(Collectors.toList());
			}
		});
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
			@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, List<String> argStrings, String prefix, boolean includeOptions) {

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
		DocoptParseResult parseResult = DocoptParseResult.parse(argStrings, optionsMap, startNode, includeOptions, optionsDisplay);
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
					if(!variableInstantiator.allowsDuplicateListSuggestions()) {
						instantiations = instantiations.stream()
								.filter(i -> !((List<String>) currentBinding).contains(i.getSuggestedWord()))
								.collect(Collectors.toList());
					}
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

	public static List<CompletionSuggestion> completePath(ConsoleCommandContext cmdContext, String prefix, boolean directoriesOnly) {
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
	
	
	private static abstract class PropertyInstantiator extends VariableInstantiator {
		
		private String tableName;

		protected PropertyInstantiator(String tableName) {
			this.tableName = tableName;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			return getProperties(cmdContext).stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
		}

		protected String getTableName() {
			return tableName;
		}
		
		protected abstract List<String> getProperties(ConsoleCommandContext cmdContext);

		protected Project getProject(ConsoleCommandContext cmdContext) {
			InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
			Project project = insideProjectMode.getProject();
			return project;
		}
	}

	public static class CustomTableNameInstantiator extends VariableInstantiator {
		

		public CustomTableNameInstantiator() {
		}

		@Override
		@SuppressWarnings("rawtypes")
		public List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			return getProject(cmdContext).getCustomTables()
					.stream()
					.map(t -> new CompletionSuggestion(t.getName(), true))
					.collect(Collectors.toList());
		}

		private Project getProject(ConsoleCommandContext cmdContext) {
			InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
			Project project = insideProjectMode.getProject();
			return project;
		}
	}

	
	public static final class CustomFieldNameInstantiator extends PropertyInstantiator {
		public CustomFieldNameInstantiator(String tableName) {
			super(tableName);
		}
		@Override
		protected List<String> getProperties(ConsoleCommandContext cmdContext) {
			return getProject(cmdContext).getCustomFieldNames(getTableName());
		}
	}

	public static final class ModifiableFieldNameInstantiator extends PropertyInstantiator {
		public ModifiableFieldNameInstantiator(String tableName) {
			super(tableName);
		}
		@Override
		protected List<String> getProperties(ConsoleCommandContext cmdContext) {
			return getProject(cmdContext).getModifiableFieldNames(getTableName());
		}
	}

	public static final class ModifiablePropertyInstantiator extends PropertyInstantiator {
		public ModifiablePropertyInstantiator(String tableName) {
			super(tableName);
		}
		@Override
		protected List<String> getProperties(ConsoleCommandContext cmdContext) {
			return getProject(cmdContext).getModifiableProperties(getTableName());
		}
	}

	public static final class ListablePropertyInstantiator extends PropertyInstantiator {
		public ListablePropertyInstantiator(String tableName) {
			super(tableName);
		}
		@Override
		protected List<String> getProperties(ConsoleCommandContext cmdContext) {
			return getProject(cmdContext).getListableProperties(getTableName());
		}
	}


}
