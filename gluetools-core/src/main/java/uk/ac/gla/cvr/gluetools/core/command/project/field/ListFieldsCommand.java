package uk.ac.gla.cvr.gluetools.core.command.project.field;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.Field;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="list-fields")
@CommandClass(description="List data fields in the current project", 
	docoptUsages={""}) 
public class ListFieldsCommand extends ProjectModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(Field.PROJECT_PROPERTY, getProjectName());
		return CommandUtils.runListCommand(cmdContext, Field.class, new SelectQuery(Field.class, exp));
	}

}
