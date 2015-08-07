package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.MemberFieldCompleter;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
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
		"  list member -w \"sequence.sequenceID like 'f%' and sequence.CUSTOM_FIELD = 'value1'\"\n"+
		"  list member sequence.sequenceID sequence.CUSTOM_FIELD"
	) 
public class ListMemberCommand extends AlignmentModeCommand<ListResult> {

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
			getAlignmentMode(cmdContext).getProject().checkValidMemberFieldNames(fieldNames);
		}
		ObjectContext objContext = cmdContext.getObjectContext();
		Expression matchAlignmentName = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, getAlignmentName());
		SelectQuery selectQuery = null;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(AlignmentMember.class, whereClause.get().andExp(matchAlignmentName));
		} else {
			selectQuery = new SelectQuery(AlignmentMember.class, matchAlignmentName);
		}
		List<AlignmentMember> members = GlueDataObject.query(objContext, AlignmentMember.class, selectQuery);
		if(fieldNames == null) {
			return new ListResult(AlignmentMember.class, members);
		} else {
			return new ListResult(AlignmentMember.class, members, fieldNames);
		}
	}

	
	@SuppressWarnings("rawtypes")
	@CompleterClass
	public static class ListMembersFieldCompleter extends MemberFieldCompleter {
		@Override
		public List<String> completionSuggestions(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass, List<String> argStrings) {
			List<String> suggestions = new ArrayList<String>();
			if(argStrings.size() == 0) {
				suggestions.add("-w");
				suggestions.add("--whereClause");
				suggestions.addAll(getMemberFieldNames(cmdContext));
			} else if(argStrings.size() == 1) {
				String arg0 = argStrings.get(0);
				if(!arg0.equals("-w") && !arg0.equals("--whereClause")) {
					suggestions.addAll(getMemberFieldNames(cmdContext));
				}
			} else {
				suggestions.addAll(getMemberFieldNames(cmdContext));
			}
			return suggestions;
		}
		
	}
	
}
