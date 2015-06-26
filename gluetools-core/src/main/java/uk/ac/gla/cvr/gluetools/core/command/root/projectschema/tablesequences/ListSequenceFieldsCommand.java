package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.tablesequences;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;


@CommandClass( 
	commandWords={"list", "fields"},
	docoptUsages={""},
	description="List the fields in the table") 
public class ListSequenceFieldsCommand extends TableSequencesModeCommand {

	
	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(Field.PROJECT_PROPERTY, getProjectName());
		return CommandUtils.runListCommand(cmdContext, Field.class, new SelectQuery(Field.class, exp));
	}

}
