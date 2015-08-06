package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.document.DocumentBuilder;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class FieldValueResult extends CommandResult {

	public FieldValueResult(String fieldName, String fieldValue) {
		super("fieldValueResult");
		DocumentBuilder documentBuilder = getDocumentBuilder();
		documentBuilder.set("fieldName", fieldName);
		if(fieldValue == null) {
			documentBuilder.setNull("value");
		} else {
			documentBuilder.set("value", fieldValue);
		}
	}
	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		buf.append(GlueXmlUtils.getXPathString(getDocument(), "/fieldValueResult/fieldName/text()"));
		buf.append(": ");
		Element valueElem = GlueXmlUtils.findChildElements(getDocument().getDocumentElement(), "value").get(0);
		if(JsonUtils.getJsonType(valueElem) == JsonType.Null) {
			buf.append("-");
		} else {
			buf.append(valueElem.getTextContent());
		}
		renderCtx.output(buf.toString());
	}
	
	
	
}
