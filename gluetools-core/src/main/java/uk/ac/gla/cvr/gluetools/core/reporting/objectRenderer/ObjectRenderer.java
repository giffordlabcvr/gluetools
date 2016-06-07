package uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;

public abstract class ObjectRenderer<P extends ObjectRenderer<P>> extends ModulePlugin<P> implements IObjectRenderer {

	public static ObjectRenderer<?> getRenderer(CommandContext cmdContext, String rendererModuleName) {
		return Module.resolveModulePlugin(cmdContext, ObjectRenderer.class, rendererModuleName);
	}

	protected abstract byte[] renderToXmlBytes(CommandContext cmdContext, GlueDataObject renderableObject);
	

	@Override
	public final DocumentResult render(CommandContext cmdContext, GlueDataObject renderableObject) {
		byte[] resultXmlBytes = renderToXmlBytes(cmdContext, renderableObject);
		return IObjectRenderer.documentResultFromBytes(resultXmlBytes);
	}
	
}
