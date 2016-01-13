package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass(
	commandWords={"list", "reference"}, 
	docoptUsages={"[-w <whereClause>]"},
	docoptOptions={
	"-w <whereClause>, --whereClause <whereClause>  Qualify result set"},
	description="List reference sequences") 
public class ListReferenceSequenceCommand extends ProjectModeCommand<ListResult> {

	public static final String WHERE_CLAUSE = "whereClause";
	private Optional<Expression> whereClause;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
	}

	@Override
	public ListResult execute(CommandContext cmdContext) {
		SelectQuery selectQuery;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(ReferenceSequence.class, whereClause.get());
		} else {
			selectQuery = new SelectQuery(ReferenceSequence.class);
		}
		return CommandUtils.runListCommand(cmdContext, ReferenceSequence.class, selectQuery);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {}

}
