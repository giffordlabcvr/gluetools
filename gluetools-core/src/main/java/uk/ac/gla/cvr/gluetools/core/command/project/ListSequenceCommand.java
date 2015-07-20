package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"list", "sequence"},
	docoptUsages={"[-w <whereClause>] [<fieldName> ...]"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>  Qualify result set"},
	description="List sequences or sequence field values",
	furtherHelp=
	"Where fieldNames are specified, only these field values will be displayed.\n"+
	"Examples:\n"+
	"  list sequence -w \"source.name = 'local'\"\n"+
	"  list sequence -w \"sequenceID like 'f%' and CUSTOM_FIELD = 'value1'\"\n"+
	"  list sequence sequenceID CUSTOM_FIELD"
) 
public class ListSequenceCommand extends ProjectModeCommand {

	public static final String FIELD_NAME = "fieldName";
	public static final String WHERE_CLAUSE = "whereClause";
	private Expression whereClause;
	private List<String> fieldNames;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		fieldNames = PluginUtils.configureStringsProperty(configElem, FIELD_NAME);
		if(fieldNames.isEmpty()) {
			fieldNames = null; // default fields
		}
	}
	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		SelectQuery selectQuery;
		if(whereClause != null) {
			selectQuery = new SelectQuery(Sequence.class, whereClause);
		} else {
			selectQuery = new SelectQuery(Sequence.class);
		}
		List<String> validFieldNamesList = getProjectMode(cmdContext).getProject().getAllSequenceFieldNames();
		Set<String> validFieldNames = new LinkedHashSet<String>(validFieldNamesList);
		if(fieldNames != null) {
			fieldNames.forEach(f-> {
				if(!validFieldNames.contains(f)) {
					throw new SequenceException(Code.INVALID_FIELD, f, validFieldNamesList);
				}
			});
		}
		return CommandUtils.runListCommand(cmdContext, Sequence.class, selectQuery, fieldNames);
	}
	
	@CompleterClass
	public static class Completer extends FieldCompleter {
		@Override
		public List<String> completionSuggestions(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass, List<String> argStrings) {
			List<String> suggestions = new ArrayList<String>();
			if(argStrings.size() == 0) {
				suggestions.add("-w");
				suggestions.add("--whereClause");
				suggestions.addAll(getAllFieldNames(cmdContext));
			} else if(argStrings.size() == 1) {
				String arg0 = argStrings.get(0);
				if(!arg0.equals("-w") && !arg0.equals("--whereClause")) {
					suggestions.addAll(getAllFieldNames(cmdContext));
				}
			} else {
				suggestions.addAll(getAllFieldNames(cmdContext));
			}
			return suggestions;
		}
		
	}


}
