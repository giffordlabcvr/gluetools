/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.Base64;

import uk.ac.gla.cvr.gluetools.core.command.result.InteractiveCommandResultRenderingContext;
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
	protected void renderToConsoleAsText(InteractiveCommandResultRenderingContext renderCtx) {
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
