package uk.ac.gla.cvr.gluetools.core.command.root.webdocs;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.root.webdocs.pojos.WebdocsModuleTypeList;


@CommandClass( 
		commandWords={"webdocs", "list-module-types"}, 
	docoptUsages={""},
	//metaTags={CmdMeta.webApiOnly}, 
	description = "")
public class WebdocsListModuleTypesCommand extends WebdocsCommand<PojoCommandResult<WebdocsModuleTypeList>> {

	@Override
	public PojoCommandResult<WebdocsModuleTypeList> execute(CommandContext cmdContext) {
		return new PojoCommandResult<WebdocsModuleTypeList>(WebdocsModuleTypeList.create());
	}
	
}
