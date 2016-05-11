package uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.ObjectRendererException.Code;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class ObjectRenderer<P extends ObjectRenderer<P>> extends ModulePlugin<P> {

	public static ObjectRenderer<?> getRenderer(CommandContext cmdContext, String rendererModuleName) {
		return Module.resolveModulePlugin(cmdContext, ObjectRenderer.class, rendererModuleName);
	}

	protected abstract byte[] renderToXmlBytes(CommandContext cmdContext, GlueDataObject renderableObject);
	
	
	public final DocumentResult render(CommandContext cmdContext, GlueDataObject renderableObject) {
		byte[] resultXmlBytes = renderToXmlBytes(cmdContext, renderableObject);
		Document xmlResultDocument = null;
		try {
			xmlResultDocument = GlueXmlUtils.documentFromBytes(resultXmlBytes);
		} catch (SAXException e) {
			throw new ObjectRendererException(e, Code.INVALID_XML_PRODUCED, e.getLocalizedMessage());
		}
		return new DocumentResult(xmlResultDocument);
	}
	
}
