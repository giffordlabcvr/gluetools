package uk.ac.gla.cvr.gluetools.core.command.root.webdocs;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos.WebdocsNonModeCommandsSummary;

@CommandClass( 
		commandWords={"webdocs", "document-non-mode-commands"}, 
	docoptUsages={""},
	metaTags={CmdMeta.webApiOnly, CmdMeta.suppressDocs}, 
	description = "")
public class WebdocsDocumentNonModeCommandsCommand extends WebdocsCommand<PojoCommandResult<WebdocsNonModeCommandsSummary>> {

	@Override
	public PojoCommandResult<WebdocsNonModeCommandsSummary> execute(CommandContext cmdContext) {
		return new PojoCommandResult<WebdocsNonModeCommandsSummary>(WebdocsNonModeCommandsSummary.create());
	}

}
