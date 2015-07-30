package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.Optional;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class FieldValueResult extends CommandResult {

	public FieldValueResult(String fieldName, String fieldValue) {
		super(fieldValueResultDocument(fieldName, fieldValue));
	}

	private static Document fieldValueResultDocument(String fieldName, String fieldValue) {
		Element rootElem = XmlUtils.documentWithElement("fieldValueResult");
		JsonUtils.setJsonType(rootElem, JsonType.Object, false);
		XmlUtils.appendElementWithText(rootElem, "fieldName", fieldName, JsonType.String);
		if(fieldValue == null) {
			Element valueElem = XmlUtils.appendElement(rootElem, "value");
			valueElem.setAttribute("isNull", "true");
			JsonUtils.setJsonType(valueElem, JsonType.Null, false);
		} else {
			XmlUtils.appendElementWithText(rootElem, "value", fieldValue, JsonType.String);
		}
		return rootElem.getOwnerDocument();
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		buf.append(XmlUtils.getXPathString(getDocument(), "/fieldValueResult/fieldName/text()"));
		buf.append(": ");
		String valueIsNull = Optional.ofNullable(XmlUtils.getXPathString(getDocument(), "/fieldValueResult/value/@isNull")).orElse("false");
		String valueString = XmlUtils.getXPathString(getDocument(), "/fieldValueResult/value/text()");
		if(valueIsNull.equals("true")) {	
			buf.append("-");
		} else {
			buf.append(valueString);

		}
		renderCtx.output(buf.toString());
	}
	
	
	
}
