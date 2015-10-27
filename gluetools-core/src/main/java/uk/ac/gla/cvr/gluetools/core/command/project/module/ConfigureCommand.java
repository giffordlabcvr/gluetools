package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


public abstract class ConfigureCommand<P extends ModulePlugin<P>> extends ModuleProvidedCommand<OkResult, P> {
	
	@Override
	protected final OkResult execute(CommandContext cmdContext, P modulePlugin) {
		
		Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(getModuleName()));
		Document currentDocument = module.getConfigDoc();
		updateDocument(cmdContext, currentDocument);
		module.setConfig(GlueXmlUtils.prettyPrint(currentDocument));
		// test that this was a valid update by building the modulePlugin again.
		module.getModulePlugin(cmdContext.getGluetoolsEngine());
		cmdContext.commit();
		return CommandResult.OK;
	}
	
	protected abstract void updateDocument(CommandContext cmdContext, Document modulePluginDoc);
	
}
