package uk.ac.gla.cvr.gluetools.core.command.project.module;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.moduleResource.ModuleResource;
@CommandClass(
		commandWords={"list", "resource"}, 
		docoptUsages={""},
		description="List stored file resources")

public class ModuleListResourceCommand extends ModuleModeCommand<BaseTableResult<ModuleResource>> {

	@Override
	public BaseTableResult<ModuleResource> execute(CommandContext cmdContext) {		
		Module module = lookupModule(cmdContext);
		List<ModuleResource> resources = module.getResources();
		
		return new BaseTableResult<ModuleResource>("moduleListResourceResult", resources, 
				BaseTableResult.column("resourceFileName", (res)->res.getName()), 
				BaseTableResult.column("bytesSize", (res)->res.getContent().length));
	}

}
