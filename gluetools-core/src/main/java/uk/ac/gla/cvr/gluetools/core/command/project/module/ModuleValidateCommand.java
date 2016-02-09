package uk.ac.gla.cvr.gluetools.core.command.project.module;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;

@CommandClass(
		commandWords={"validate"}, 
		description = "Validate the module's configuration", 
		docoptUsages = {""}, 
		docoptOptions = {}
)
public class ModuleValidateCommand extends ModuleDocumentCommand<OkResult> {

	@Override
	protected OkResult execute(CommandContext cmdContext, Module module) {
		module.validate(cmdContext);
		return new OkResult();
	}

}
