package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.apache.cayenne.ObjectContext;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;


public abstract class ShowConfigCommand<P extends ModulePlugin<P>> extends ModuleProvidedCommand<P> {

	@Override
	protected final CommandResult execute(CommandContext cmdContext, P modulePlugin) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Module module = GlueDataObject.lookup(objContext, Module.class, Module.pkMap(getModuleName()));
		return new DocumentResult(module.getConfigDoc());
	}
}
