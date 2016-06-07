package uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.ObjectRendererException.Code;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public interface IObjectRenderer {

	public DocumentResult render(CommandContext cmdContext, GlueDataObject renderableObject);

	public static DocumentResult documentResultFromBytes(byte[] resultXmlBytes) {
		Document xmlResultDocument = null;
		try {
			xmlResultDocument = GlueXmlUtils.documentFromBytes(resultXmlBytes);
		} catch (SAXException e) {
			throw new ObjectRendererException(e, Code.INVALID_XML_PRODUCED, e.getLocalizedMessage());
		}
		return new DocumentResult(xmlResultDocument);
	}

}
