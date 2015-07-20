package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DocumentResult;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class FieldValueResult extends DocumentResult {

	public FieldValueResult(String fieldName, String fieldValue) {
		super(fieldValueResultDocument(fieldName, fieldValue));
	}

	private static Document fieldValueResultDocument(String fieldName, String fieldValue) {
		Element rootElem = XmlUtils.documentWithElement("fieldValueResult");
		XmlUtils.appendElementWithText(rootElem, "fieldName", fieldName);
		if(fieldValue == null) {
			XmlUtils.appendElement(rootElem, "null");
		} else {
			XmlUtils.appendElementWithText(rootElem, "value", fieldValue);
		}
		return rootElem.getOwnerDocument();
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		buf.append(XmlUtils.getXPathString(getDocument(), "/fieldValueResult/fieldName/text()"));
		buf.append(": ");
		String valueString = XmlUtils.getXPathString(getDocument(), "/fieldValueResult/value/text()");
		if(valueString != null) {	
			buf.append(valueString);
		} else {
			buf.append("-");
		}
		renderCtx.output(buf.toString());
	}
	
	
	
}
