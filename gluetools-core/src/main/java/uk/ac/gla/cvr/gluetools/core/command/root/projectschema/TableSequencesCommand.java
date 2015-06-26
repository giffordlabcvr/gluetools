package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.tablesequences.TableSequencesMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;


@CommandClass( 
	commandWords={"table", "SEQUENCES"},
	docoptUsages={""},
	description="Enter command mode to manage the SEQUENCES table") 
public class TableSequencesCommand extends ProjectSchemaModeCommand implements EnterModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Project project = getProjectSchemaMode(cmdContext).getProject();
		cmdContext.pushCommandMode(new TableSequencesMode(cmdContext, project));
		return CommandResult.OK;
	}

}
