package uk.ac.gla.cvr.gluetools.core.digs.importer;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;

@CommandClass(
		commandWords={"list", "digs-databases"}, 
		description = "List all available DIGS databases", 
		docoptUsages = { "" },
		docoptOptions = { },
		metaTags = {}	
)
public class ListDigsDbsCommand extends DigsImporterCommand<ListDigsDbsResult> implements ProvidedProjectModeCommand {
	@Override
	protected ListDigsDbsResult execute(CommandContext cmdContext, DigsImporter digsImporter) {
		return new ListDigsDbsResult(cmdContext, DigsImporter.listDigsDatabases(cmdContext));
	}

	
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
		
	}
	
}
