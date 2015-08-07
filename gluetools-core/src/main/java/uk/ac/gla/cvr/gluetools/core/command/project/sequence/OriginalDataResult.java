package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.Base64;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.document.DocumentBuilder;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class OriginalDataResult extends CommandResult {

	public OriginalDataResult(SequenceFormat format, byte[] originalData) {
		super("originalDataResult");
		DocumentBuilder builder = getDocumentBuilder();
		builder.setString("format", format.name());
		builder.setString("base64", new String(Base64.getEncoder().encode(originalData)));
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		String formatName = getFormatString();
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
		return GlueXmlUtils.getXPathString(getDocument(), "/originalDataResult/base64/text()");
	}

	public String getFormatString() {
		return GlueXmlUtils.getXPathString(getDocument(), "/originalDataResult/format/text()");
	}
	
	public SequenceFormat getFormat() {
		return SequenceFormat.valueOf(getFormatString());
	}
}
