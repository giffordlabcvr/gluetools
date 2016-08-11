package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;

@CommandClass( 
		commandWords={"list", "custom-table"},
		docoptUsages={""},
		description="List the custom tables in the project") 
public class ListCustomTableCommand extends ProjectSchemaModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(CustomTable.PROJECT_PROPERTY, getProjectName());
		return CommandUtils.runListCommand(cmdContext, CustomTable.class, new SelectQuery(CustomTable.class, exp));
	}

}

