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
package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;


@CommandClass(
		commandWords={"validate-submission"}, 
		description = "Show GenBank validation messages for set of stored GLUE sequences", 
		docoptUsages = { "(-w <whereClause> | -a | -s <sourceName> <sequenceID>) -t <templateFile> [-d <dataDir>]" },
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>     Qualify the sequence set",
				"-a, --allSequences                                All sequences in the project",
				"-s, --specificSequence                            Specific <sourceName> + <sequenceID>",
				"-t <templateFile>, --templateFile <templateFile>  Template .sbt file",
				"-d <dataDir>, --dataDir <dataDir>                 Directory for intermediate files",
		},
		furtherHelp = "This command uses tbl2asn as a subroutine to check for validation messages. "+
		"If <outputDir> is omitted, the files are written to the current load/save path. If <outputDir> does not "+
		"exist, it is created. If <dataDir> is supplied, it is created if it does not exist and the the intermediate "+
		"files which were supplied to tbl2asn are retained in that directory.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class GbSubmissionValidateCommand extends BaseGbSubmissionGeneratorCommand<Void, List<GbSubmissionValidateCommand.SequenceValidationMessage>, GbSubmissionValidateResult> {
	
	public GbSubmissionValidateCommand() {
		super();
		setValidate(true);
	}


	@CompleterClass
	public static class Completer extends GbSubmissionGeneratorCompleter {}
	
	/**
	 * summary for a single sequence.
	 */
	public static class SequenceValidationMessage {
		
		private String sourceName;
		private String sequenceID;
		private String level;
		private String code;
		private String message;
		private SequenceValidationMessage(String sourceName, String sequenceID, String level, String code, String message) {
			super();
			this.sourceName = sourceName;
			this.sequenceID = sequenceID;
			this.level = level;
			this.code = code;
			this.message = message;
		}
		public String getSourceName() {
			return sourceName;
		}
		public String getSequenceID() {
			return sequenceID;
		}
		public String getLevel() {
			return level;
		}
		public String getCode() {
			return code;
		}
		public String getMessage() {
			return message;
		}
		
		
	}

	@Override
	protected List<SequenceValidationMessage> intermediateResult(Void context, Tbl2AsnResult tbl2AsnResult) {

		byte[] valBytes = tbl2AsnResult.getValFileContent();
		
		String valString = new String(valBytes);
		String[] msgLines = valString.split("\\n");
		List<SequenceValidationMessage> intermediateResult = new ArrayList<SequenceValidationMessage>();
		for(String msgLine: msgLines) {
			String msgLineTrimmed = msgLine.trim();
			if(msgLineTrimmed.length() > 0) {
				Pattern pattern = Pattern.compile("([A-Z_]+):\\s+[a-z]+\\s+\\[([^\\]]+)\\]\\s+(.*)");
				Matcher matcher = pattern.matcher(msgLineTrimmed);
				if(matcher.find()) {
					String level = matcher.group(1);
					String code = matcher.group(2);
					String message = matcher.group(3);
					SequenceValidationMessage svm = new SequenceValidationMessage(tbl2AsnResult.getSourceName(), tbl2AsnResult.getSequenceID(), level, code, message);
					intermediateResult.add(svm);
				} else {
					throw new Tbl2AsnException(Tbl2AsnException.Code.TBL2ASN_DATA_EXCEPTION, 
							"Line in .val file has unexpected structure: "+msgLineTrimmed);
				}
			}
		}
		return intermediateResult;
	}


	@Override
	protected GbSubmissionValidateResult finalResult(List<List<SequenceValidationMessage>> intermediateResults) {
		List<SequenceValidationMessage> flattenedList = intermediateResults.stream()
		        .flatMap(List::stream)
		        .collect(Collectors.toList());
		return new GbSubmissionValidateResult(flattenedList);
	}


	@Override
	protected Void initContext(ConsoleCommandContext consoleCmdContext) {
		return null;
	}


}
