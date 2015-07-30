package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.Base64;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class OriginalDataResult extends CommandResult {

	public OriginalDataResult(SequenceFormat format, byte[] originalData) {
		super(originalDataResultDocument(format, originalData));
	}

	private static Document originalDataResultDocument(SequenceFormat format, byte[] originalData) {
		Element rootElem = XmlUtils.documentWithElement("originalDataResult");
		JsonUtils.setJsonType(rootElem, JsonType.Object, false);
		XmlUtils.appendElementWithText(rootElem, "format", format.name(), JsonType.String);
		String base64String = new String(Base64.getEncoder().encode(originalData));
		XmlUtils.appendElementWithText(rootElem, "base64", base64String, JsonType.String);
		return rootElem.getOwnerDocument();
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		String formatName = getFormat();
		buf.append("Format: ");
		buf.append(formatName);
		buf.append("\n");
		byte[] originalData = getBase64Bytes();
		SequenceFormat format = SequenceFormat.valueOf(formatName);
		buf.append(format.originalDataAsString(originalData));
		renderCtx.output(buf.toString());
	}

	public byte[] getBase64Bytes() {
		return Base64.getDecoder().decode(getBase64String());
	}

	public String getBase64String() {
		return XmlUtils.getXPathString(getDocument(), "/originalDataResult/base64/text()");
	}

	public String getFormat() {
		return XmlUtils.getXPathString(getDocument(), "/originalDataResult/format/text()");
	}
	
}
