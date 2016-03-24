package uk.ac.gla.cvr.gluetools.core.command.project.sequenceGroup;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.groupMember.GroupMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"list", "member"},
		docoptUsages={"[-w <whereClause>] [<fieldName> ...]"},
		docoptOptions={
			"-w <whereClause>, --whereClause <whereClause>  Qualify result set"},
		description="List member sequences or field values",
		furtherHelp=
		"The optional whereClause qualifies which alignment member are displayed.\n"+
		"If whereClause is not specified, all alignment members are displayed.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  list member -w \"sequence.source.name = 'local'\"\n"+
		"  list member -w \"sequence.sequenceID like 'f%' and sequence.custom_field = 'value1'\"\n"+
		"  list member sequence.sequenceID sequence.custom_field"
	) 
public class ListMemberCommand extends GroupModeCommand<ListResult> {

	public static final String FIELD_NAME = "fieldName";
	public static final String WHERE_CLAUSE = "whereClause";
	private Optional<Expression> whereClause;
	private List<String> fieldNames;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		fieldNames = PluginUtils.configureStringsProperty(configElem, FIELD_NAME);
		if(fieldNames.isEmpty()) {
			fieldNames = null; // default fields
		}
	}

	
	@Override
	public ListResult execute(CommandContext cmdContext) {
		if(fieldNames != null) {
			getGroupMode(cmdContext).getProject().checkListableMemberField(fieldNames);
		}
		
		Expression matchGroupName = ExpressionFactory.matchExp(GroupMember.GROUP_NAME_PATH, getGroupName());
		SelectQuery selectQuery = null;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(GroupMember.class, whereClause.get().andExp(matchGroupName));
		} else {
			selectQuery = new SelectQuery(GroupMember.class, matchGroupName);
		}
		List<GroupMember> members = GlueDataObject.query(cmdContext, GroupMember.class, selectQuery);
		if(fieldNames == null) {
			return new ListResult(GroupMember.class, members);
		} else {
			return new ListResult(GroupMember.class, members, fieldNames);
		}
	}

	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		
		public Completer() {
			super();
			registerVariableInstantiator("fieldName", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return getMemberFieldNames(cmdContext).stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
				}
				protected List<String> getMemberFieldNames(ConsoleCommandContext cmdContext) {
					return getProject(cmdContext).getListableMemberFields();
				}
				private Project getProject(ConsoleCommandContext cmdContext) {
					InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
					Project project = insideProjectMode.getProject();
					return project;
				}
			});
		}
	}
	
}
