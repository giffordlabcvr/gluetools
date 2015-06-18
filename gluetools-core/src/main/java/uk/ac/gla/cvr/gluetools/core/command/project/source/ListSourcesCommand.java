package uk.ac.gla.cvr.gluetools.core.command.project.source;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="list-sources")
@CommandClass(description="List sequence sources in the current project", 
	docoptUsages={""}) 
public class ListSourcesCommand extends ProjectModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(Source.PROJECT_PROPERTY, getProjectName());
		return CommandUtils.runListCommand(cmdContext, Source.class, new SelectQuery(Source.class, exp));
	}

}
