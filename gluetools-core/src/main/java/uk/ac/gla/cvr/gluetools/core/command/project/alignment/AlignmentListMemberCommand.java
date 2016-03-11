package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

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
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"list", "member"},
		docoptUsages={"[-r] [-w <whereClause>] [<fieldName> ...]"},
		docoptOptions={
				"-r, --recursive                                Include descendent members",
				"-w <whereClause>, --whereClause <whereClause>  Qualify result set",
			},
		description="List member sequences or field values",
		furtherHelp=
		"The optional whereClause qualifies which alignment member are displayed.\n"+
		"If whereClause is not specified, all alignment members are displayed.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  list member -w \"sequence.source.name = 'local'\"\n"+
		"  list member -w \"sequence.sequenceID like 'f%' and sequence.CUSTOM_FIELD = 'value1'\"\n"+
		"  list member sequence.sequenceID sequence.CUSTOM_FIELD"
	) 
public class AlignmentListMemberCommand extends AlignmentModeCommand<ListResult> {

	public static final String RECURSIVE = "recursive";
	public static final String FIELD_NAME = "fieldName";
	public static final String WHERE_CLAUSE = "whereClause";
	
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private List<String> fieldNames;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		recursive = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, RECURSIVE, false)).orElse(false);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		fieldNames = PluginUtils.configureStringsProperty(configElem, FIELD_NAME);
		if(fieldNames.isEmpty()) {
			fieldNames = null; // default fields
		}
	}

	
	@Override
	public ListResult execute(CommandContext cmdContext) {
		if(fieldNames != null) {
			getAlignmentMode(cmdContext).getProject().checkListableMemberField(fieldNames);
		}

		List<AlignmentMember> members = listMembers(cmdContext, lookupAlignment(cmdContext), recursive, whereClause);
		if(fieldNames == null) {
			return new ListResult(AlignmentMember.class, members);
		} else {
			return new ListResult(AlignmentMember.class, members, fieldNames);
		}
	}


	public static List<AlignmentMember> listMembers(CommandContext cmdContext,
			Alignment alignment, Boolean recursive, Optional<Expression> whereClause) {
		Expression matchAlignmentOrDescendent = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignment.getName());
		if(recursive) {
			List<Alignment> descendents = alignment.getDescendents();
			for(Alignment descAlignment: descendents) {
				matchAlignmentOrDescendent = matchAlignmentOrDescendent.orExp(
						ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, descAlignment.getName()));
			}
		}
		
		SelectQuery selectQuery = null;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(AlignmentMember.class, whereClause.get().andExp(matchAlignmentOrDescendent));
		} else {
			selectQuery = new SelectQuery(AlignmentMember.class, matchAlignmentOrDescendent);
		}
		List<AlignmentMember> members = GlueDataObject.query(cmdContext, AlignmentMember.class, selectQuery);
		return members;
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
