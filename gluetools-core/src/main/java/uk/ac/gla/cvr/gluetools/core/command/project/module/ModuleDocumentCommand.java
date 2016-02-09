package uk.ac.gla.cvr.gluetools.core.command.project.module;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;

public abstract class ModuleDocumentCommand<R extends CommandResult> extends ModuleModeCommand<R> {

	@SuppressWarnings("unchecked")
	@Override
	public final R execute(CommandContext cmdContext) {
		Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(getModuleName()));
		return execute(cmdContext, module);
	}

	protected abstract R execute(CommandContext cmdContext, Module module) ;
	
}
