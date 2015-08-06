package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.tablesequences;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;


@CommandClass( 
	commandWords={"list", "field"},
	docoptUsages={""},
	description="List the fields in the table") 
public class ListSequenceFieldCommand extends TableSequencesModeCommand<ListResult> {

	
	
	@Override
	public ListResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(Field.PROJECT_PROPERTY, getProjectName());
		return CommandUtils.runListCommand(cmdContext, Field.class, new SelectQuery(Field.class, exp));
	}

}
