package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.Base64;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;

public class OriginalDataResult extends MapResult {

	public OriginalDataResult(SequenceFormat format, byte[] originalData) {
		super("originalDataResult", mapBuilder()
				.put("format", format.name())
				.put("base64", new String(Base64.getEncoder().encode(originalData))));
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		String formatName = getFormatString();
		buf.append("Format: ");
		buf.append(formatName);
		buf.append("\n");
		byte[] originalData = getBase64Bytes();
		buf.append(new String(originalData));
		renderCtx.output(buf.toString());
	}

	public byte[] getBase64Bytes() {
		return Base64.getDecoder().decode(getBase64String());
	}

	public String getBase64String() {
		return getCommandDocument().getString("base64");
	}

	public String getFormatString() {
		return getCommandDocument().getString("format");
	}
	
	public SequenceFormat getFormat() {
		return SequenceFormat.valueOf(getFormatString());
	}

	public AbstractSequenceObject getSequenceObject() {
		byte[] refSeqBytes = getBase64Bytes();
		AbstractSequenceObject refSeqObject = getFormat().sequenceObject();
		refSeqObject.fromOriginalData(refSeqBytes);
		return refSeqObject;
	}
}
