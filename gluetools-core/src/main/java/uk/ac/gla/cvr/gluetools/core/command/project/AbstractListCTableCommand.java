package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class AbstractListCTableCommand<T extends GlueDataObject> extends ProjectModeCommand<ListResult> {

	public static final String FIELD_NAME = "fieldName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String PAGE_SIZE = "pageSize";
	public static final String FETCH_LIMIT = "fetchLimit";
	public static final String FETCH_OFFSET = "fetchOffset";
	private AbstractListCTableDelegate<T> listCTableDelegate = new AbstractListCTableDelegate<T>();
	
	protected AbstractListCTableCommand(ConfigurableTable cTable) {
		super();
		this.listCTableDelegate.cTable = cTable;
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		listCTableDelegate.configure(pluginConfigContext, configElem);
	}
	
	@Override
	public ListResult execute(CommandContext cmdContext) {
		return listCTableDelegate.execute(cmdContext);
	}
	
	public static class FieldNameCompleter extends AdvancedCmdCompleter {

		public FieldNameCompleter(ConfigurableTable cTable) {
			super();
			registerVariableInstantiator("fieldName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
					List<String> listableFieldNames = insideProjectMode.getProject().getListableProperties(cTable);
					return listableFieldNames.stream().map(n -> new CompletionSuggestion(n, true)).collect(Collectors.toList());
				}
			});
		}
	}
		
	
	protected void setSortComparator(Comparator<T> comparator) {
		this.listCTableDelegate.sortComparator = comparator;
	}

	public static class AbstractListCTableDelegate<T extends GlueDataObject> {
		private ConfigurableTable cTable;
		private Comparator<T> sortComparator = null;
		
		private Optional<Expression> whereClause;
		private List<String> fieldNames;
		private int pageSize;
		private Optional<Integer> fetchLimit;
		private Optional<Integer> fetchOffset;
		
		public Optional<Expression> getWhereClause() {
			return whereClause;
		}

		public void setWhereClause(Optional<Expression> whereClause) {
			this.whereClause = whereClause;
		}

		public void setcTable(ConfigurableTable cTable) {
			this.cTable = cTable;
		}

		public List<String> getFieldNames() {
			return fieldNames;
		}

		public void setFieldNames(List<String> fieldNames) {
			this.fieldNames = fieldNames;
		}

		public void setSortComparator(Comparator<T> sortComparator) {
			this.sortComparator = sortComparator;
		}

		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
			fieldNames = PluginUtils.configureStringsProperty(configElem, FIELD_NAME);
			if(fieldNames.isEmpty()) {
				fieldNames = null; // default fields
			}
			pageSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, PAGE_SIZE, false)).orElse(250);
			fetchLimit = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, FETCH_LIMIT, false));
			fetchOffset = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, FETCH_OFFSET, false));
		}

		@SuppressWarnings("unchecked")
		public ListResult execute(CommandContext cmdContext) {
			SelectQuery selectQuery;
			Class<T> dataObjectClass = (Class<T>) cTable.getDataObjectClass();
			if(whereClause.isPresent()) {
				selectQuery = new SelectQuery(dataObjectClass, whereClause.get());
			} else {
				selectQuery = new SelectQuery(dataObjectClass);
			}
			selectQuery.setPageSize(pageSize);
			fetchLimit.ifPresent(limit -> selectQuery.setFetchLimit(limit));
			fetchOffset.ifPresent(offset -> selectQuery.setFetchOffset(offset));
			InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
			Project project = insideProjectMode.getProject();
			List<T> resultDataObjects = (List<T>) GlueDataObject.query(cmdContext, dataObjectClass, selectQuery);
			if(sortComparator != null) {
				Collections.sort(resultDataObjects, sortComparator);
			}
			if(fieldNames == null) {
				return new ListResult(dataObjectClass, resultDataObjects);
			} else {
				project.checkListableProperties(cTable, fieldNames);
				return new ListResult(dataObjectClass, resultDataObjects, fieldNames);
			}
		}
	}

}
