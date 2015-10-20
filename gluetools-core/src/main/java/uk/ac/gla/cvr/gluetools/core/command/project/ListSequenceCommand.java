package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"list", "sequence"},
	docoptUsages={"[-w <whereClause>] [<fieldName> ...]"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>  Qualify result set"},
	description="List sequences or sequence field values",
	furtherHelp=
	"The optional whereClause qualifies which sequences are displayed.\n"+
	"Where fieldNames are specified, only these field values will be displayed.\n"+
	"Examples:\n"+
	"  list sequence -w \"source.name = 'local'\"\n"+
	"  list sequence -w \"sequenceID like 'f%' and CUSTOM_FIELD = 'value1'\"\n"+
	"  list sequence sequenceID CUSTOM_FIELD"
) 
public class ListSequenceCommand extends ProjectModeCommand<ListResult> {

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
		SelectQuery selectQuery;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(Sequence.class, whereClause.get());
		} else {
			selectQuery = new SelectQuery(Sequence.class);
		}
		Project project = getProjectMode(cmdContext).getProject();
		if(fieldNames == null) {
			return CommandUtils.runListCommand(cmdContext, Sequence.class, selectQuery);
		} else {
			project.checkValidSequenceFieldNames(fieldNames);
			return CommandUtils.runListCommand(cmdContext, Sequence.class, selectQuery, fieldNames);
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
					return getSequenceFieldNames(cmdContext).stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
				}
				protected List<String> getSequenceFieldNames(ConsoleCommandContext cmdContext) {
					return getProject(cmdContext).getAllSequenceFieldNames();
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
