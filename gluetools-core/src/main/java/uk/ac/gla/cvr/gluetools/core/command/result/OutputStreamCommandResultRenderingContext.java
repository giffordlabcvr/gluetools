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
package uk.ac.gla.cvr.gluetools.core.command.result;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

public class OutputStreamCommandResultRenderingContext implements CommandResultRenderingContext {
	private PrintWriter printWriter;
	private ResultOutputFormat consoleOutputFormat;
	private LineFeedStyle lineFeedStyle;
	private boolean renderTableHeaders;
	private String nullRenderingString = "-";
	private boolean trimNullValues = false;

	
	public OutputStreamCommandResultRenderingContext(OutputStream outputStream, ResultOutputFormat consoleOutputFormat,
			LineFeedStyle lineFeedStyle, boolean renderTableHeaders) {
		this(outputStream, consoleOutputFormat, lineFeedStyle, renderTableHeaders, Charset.defaultCharset().name());
	}
		
	public OutputStreamCommandResultRenderingContext(OutputStream outputStream, ResultOutputFormat consoleOutputFormat,
			LineFeedStyle lineFeedStyle, boolean renderTableHeaders, String charsetName) {
		try {
			this.printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, charsetName)), false);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		this.consoleOutputFormat = consoleOutputFormat;
		this.lineFeedStyle = lineFeedStyle;
		this.renderTableHeaders = renderTableHeaders;
	}
	
	@Override
	public void output(String message) {
		output(message, true);
	}

	@Override
	public void output(String message, boolean newLine) {
		printWriter.print(message);
		if(newLine) {
			printWriter.print(lineFeedStyle.getLineBreakChars());
		}
		printWriter.flush();
	}

	@Override
	public ResultOutputFormat getResultOutputFormat() {
		return this.consoleOutputFormat;
	}

	@Override
	public boolean renderTableHeaders() {
		return renderTableHeaders;
	}

	public void setNullRenderingString(String nullRenderingString) {
		this.nullRenderingString = nullRenderingString;
	}

	public void setTrimNullValues(boolean trimNullValues) {
		this.trimNullValues = trimNullValues;
	}

	@Override
	public String renderNull() {
		return nullRenderingString;
	}

	@Override
	public boolean trimNullValues() {
		return trimNullValues;
	}

	
	
	
	
}