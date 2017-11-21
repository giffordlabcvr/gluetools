package uk.ac.gla.cvr.gluetools.core.command.root.webdocs;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos.WebdocsCommandModeTree;

@CommandClass( 
		commandWords={"webdocs", "list-command-modes"}, 
	docoptUsages={""},
	//metaTags={CmdMeta.webApiOnly}, 
	description = "")
public class WebdocsListCommandModesCommand extends WebdocsCommand<PojoCommandResult<WebdocsCommandModeTree>> {

	@Override
	public PojoCommandResult<WebdocsCommandModeTree> execute(CommandContext cmdContext) {
		RootCommandFactory rootCommandFactory = CommandFactory.get(RootCommandFactory.class);
		return new PojoCommandResult<WebdocsCommandModeTree>(WebdocsCommandModeTree.create("/", "root", rootCommandFactory));
	}

	
	
}
