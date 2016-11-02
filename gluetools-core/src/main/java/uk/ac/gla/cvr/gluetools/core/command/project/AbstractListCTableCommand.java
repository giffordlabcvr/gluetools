package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CayenneUtils;


public abstract class AbstractListCTableCommand extends ProjectModeCommand<ListResult> {

	public static final String FIELD_NAME = "fieldName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String PAGE_SIZE = "pageSize";
	public static final String FETCH_LIMIT = "fetchLimit";
	public static final String FETCH_OFFSET = "fetchOffset";
	public static final String SORT_PROPERTIES = "sortProperties";
	private AbstractListCTableDelegate listCTableDelegate = new AbstractListCTableDelegate();
	
	protected AbstractListCTableCommand() {
		super();
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		listCTableDelegate.configure(pluginConfigContext, configElem);
	}
	
	protected void setTableName(String tableName) {
		listCTableDelegate.tableName = tableName;
	}
	
	@Override
	public ListResult execute(CommandContext cmdContext) {
		@SuppressWarnings("unused")
		long startTime = System.currentTimeMillis();
		ListResult listResult = listCTableDelegate.execute(cmdContext);
		// System.out.println("Time spent in AbstractListCTableCommand: "+(System.currentTimeMillis()-startTime));
		return listResult;
	}
	
	public static class ListCommandCompleter extends AdvancedCmdCompleter {

		public ListCommandCompleter(String tableName) {
			super();
			registerVariableInstantiator("fieldName", new ListablePropertyInstantiator(tableName));
		}
	}
		
	
	protected void setSortComparator(Comparator<? extends GlueDataObject> comparator) {
		this.listCTableDelegate.sortComparator = comparator;
	}

	public static class AbstractListCTableDelegate {
		private String tableName;
		private Comparator<? extends GlueDataObject> sortComparator = null;
		
		private Optional<Expression> whereClause;
		private List<String> fieldNames;
		private int pageSize;
		private Optional<Integer> fetchLimit;
		private Optional<Integer> fetchOffset;
		private String sortProperties;
		
		public Optional<Expression> getWhereClause() {
			return whereClause;
		}

		public void setWhereClause(Optional<Expression> whereClause) {
			this.whereClause = whereClause;
		}

		public List<String> getFieldNames() {
			return fieldNames;
		}

		public void setFieldNames(List<String> fieldNames) {
			this.fieldNames = fieldNames;
		}

		public void setSortComparator(Comparator<? extends GlueDataObject> sortComparator) {
			this.sortComparator = sortComparator;
		}

		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
			fieldNames = PluginUtils.configureStringsProperty(configElem, FIELD_NAME);
			if(fieldNames.isEmpty()) {
				fieldNames = null; // default fields
			}
			sortProperties = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, SORT_PROPERTIES, false)).orElse(null);
			pageSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, PAGE_SIZE, false)).orElse(250);
			fetchLimit = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, FETCH_LIMIT, false));
			fetchOffset = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, FETCH_OFFSET, false));
		}

		@SuppressWarnings("unchecked")
		public <D extends GlueDataObject> ListResult execute(CommandContext cmdContext) {
			SelectQuery selectQuery;
			InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
			Project project = insideProjectMode.getProject();
			Class<D> dataObjectClass = (Class<D>) project.getDataObjectClass(tableName);
			if(whereClause.isPresent()) {
				selectQuery = new SelectQuery(dataObjectClass, whereClause.get());
			} else {
				selectQuery = new SelectQuery(dataObjectClass);
			}
			selectQuery.setPageSize(pageSize);
			fetchLimit.ifPresent(limit -> selectQuery.setFetchLimit(limit));
			fetchOffset.ifPresent(offset -> selectQuery.setFetchOffset(offset));
			if(sortProperties != null) {
				selectQuery.addOrderings(CayenneUtils.sortPropertiesToOrderings(sortProperties));
			}
			List<D> resultDataObjects = GlueDataObject.query(cmdContext, dataObjectClass, selectQuery);
			if(sortComparator != null) {
				Collections.sort(resultDataObjects, (Comparator<D>) sortComparator);
			}
			@SuppressWarnings("unused")
			long startTime = System.currentTimeMillis();
			ListResult listResult = null;
			if(fieldNames == null) {
				listResult = new ListResult(cmdContext, dataObjectClass, resultDataObjects);
			} else {
				// this check would need to be more sophisticated to allow nested property paths.
				// project.checkListableProperties(tableName, fieldNames);
				listResult = new ListResult(cmdContext, dataObjectClass, resultDataObjects, fieldNames);
			}
			//System.out.println("Time spent initializing ListResult: "+(System.currentTimeMillis() - startTime));
			return listResult;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		public void setSortProperties(String sortProperties) {
			this.sortProperties = sortProperties;
		}
		
	}

}
