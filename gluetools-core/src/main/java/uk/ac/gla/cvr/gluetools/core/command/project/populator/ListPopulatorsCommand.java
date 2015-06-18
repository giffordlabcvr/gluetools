package uk.ac.gla.cvr.gluetools.core.command.project.populator;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.Populator;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="list-populators")
@CommandClass(description="List field populators in the current project", 
	docoptUsages={""}) 
public class ListPopulatorsCommand extends ProjectModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(Populator.PROJECT_PROPERTY, getProjectName());
		return CommandUtils.runListCommand(cmdContext, Populator.class, new SelectQuery(Populator.class, exp));
	}

}
