package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class ModuleDocumentCommand<R extends CommandResult> extends ModuleModeCommand<R> {

	@SuppressWarnings("unchecked")
	@Override
	public final R execute(CommandContext cmdContext) {
		Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(getModuleName()));
		Document currentDocument = module.getConfigDoc();
		R result = processDocument(cmdContext, module, currentDocument);
		if(this instanceof ModuleUpdateDocumentCommand) {
			GlueXmlUtils.stripWhitespace(currentDocument);
			module.setConfig(GlueXmlUtils.prettyPrint(currentDocument));
			cmdContext.commit();
		}
		return result;
	}
	
	protected abstract R processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc);
	
	
}
