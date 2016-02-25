package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class AbstractListCTableCommand extends ProjectModeCommand<ListResult> {

	public static final String FIELD_NAME = "fieldName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String PAGE_SIZE = "pageSize";
	public static final String FETCH_LIMIT = "fetchLimit";
	public static final String FETCH_OFFSET = "fetchOffset";
	private Optional<Expression> whereClause;
	private List<String> fieldNames;
	private int pageSize;
	private Optional<Integer> fetchLimit;
	private Optional<Integer> fetchOffset;
	private ConfigurableTable cTable;
	
	protected AbstractListCTableCommand(ConfigurableTable cTable) {
		super();
		this.cTable = cTable;
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		fieldNames = PluginUtils.configureStringsProperty(configElem, FIELD_NAME);
		if(fieldNames.isEmpty()) {
			fieldNames = null; // default fields
		}
		pageSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, PAGE_SIZE, false)).orElse(250);
		fetchLimit = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, FETCH_LIMIT, false));
		fetchOffset = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, FETCH_OFFSET, false));
	}
	
	@Override
	public ListResult execute(CommandContext cmdContext) {
		SelectQuery selectQuery;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(cTable.getClass(), whereClause.get());
		} else {
			selectQuery = new SelectQuery(cTable.getClass());
		}
		selectQuery.setPageSize(pageSize);
		fetchLimit.ifPresent(limit -> selectQuery.setFetchLimit(limit));
		fetchOffset.ifPresent(offset -> selectQuery.setFetchOffset(offset));
		Project project = getProjectMode(cmdContext).getProject();
		if(fieldNames == null) {
			return CommandUtils.runListCommand(cmdContext, Sequence.class, selectQuery);
		} else {
			project.checkListableFieldNames(cTable, fieldNames);
			return CommandUtils.runListCommand(cmdContext, Sequence.class, selectQuery, fieldNames);
		}
	}


	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		

		public Completer() {
			super();
			registerVariableInstantiator("fieldName", new SequenceFieldNameInstantiator());
		}
	}
		


}
