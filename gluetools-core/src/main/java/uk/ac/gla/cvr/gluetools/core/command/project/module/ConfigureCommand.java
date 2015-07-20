package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;


public abstract class ConfigureCommand<P extends ModulePlugin<P>> extends ModuleProvidedCommand<P> {
	
	@Override
	protected final CommandResult execute(CommandContext cmdContext, P modulePlugin) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Module module = GlueDataObject.lookup(objContext, Module.class, Module.pkMap(getModuleName()));
		Document currentDocument = module.getConfigDoc();
		updateDocument(cmdContext, currentDocument);
		module.setConfig(XmlUtils.prettyPrint(currentDocument));
		// test that this was a valid update by building the modulePlugin again.
		module.getModulePlugin(cmdContext.getGluetoolsEngine());
		return CommandResult.OK;
	}
	
	protected abstract void updateDocument(CommandContext cmdContext, Document modulePluginDoc);
	
}
