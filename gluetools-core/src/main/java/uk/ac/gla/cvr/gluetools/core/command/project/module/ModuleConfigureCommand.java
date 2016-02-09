package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


public abstract class ModuleConfigureCommand extends ModuleDocumentCommand<OkResult> {
	
	@Override
	protected final OkResult execute(CommandContext cmdContext, Module module) {
		Document currentDocument = module.getConfigDoc();
		updateDocument(cmdContext, module, currentDocument);
		GlueXmlUtils.stripWhitespace(currentDocument);
		module.setConfig(GlueXmlUtils.prettyPrint(currentDocument));
		cmdContext.commit();
		return CommandResult.OK;
	}
	
	protected abstract void updateDocument(CommandContext cmdContext, Module module, Document modulePluginDoc);
	
}
