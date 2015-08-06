package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.apache.cayenne.ObjectContext;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


public abstract class ShowConfigCommand<P extends ModulePlugin<P>> extends ModuleProvidedCommand<ConsoleCommandResult, P> {

	@Override
	protected final ConsoleCommandResult execute(CommandContext cmdContext, P modulePlugin) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Module module = GlueDataObject.lookup(objContext, Module.class, Module.pkMap(getModuleName()));
		return new ConsoleCommandResult(new String(GlueXmlUtils.prettyPrint(module.getConfigDoc())));
	}
}
