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

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;


@CommandClass(
		commandWords={"preview-gbf"}, 
		description = "Preview the .gbf (GenBank flat) file for a single GLUE sequences", 
		docoptUsages = { "-s <sourceName> <sequenceID> -t <templateFile> [-d <dataDir>]" },
		docoptOptions = { 
				"-s, --specificSequence                            Specific <sourceName> + <sequenceID>",
				"-t <templateFile>, --templateFile <templateFile>  Template .sbt file",
				"-d <dataDir>, --dataDir <dataDir>                 Directory for intermediate files",
		},
		furtherHelp = "This command uses tbl2asn as a subroutine to generate a .gbf (GenBank flat) file, "+
		"which is previewed on the console. If <dataDir> is supplied, it is created if it does not exist and the the intermediate "+
		"files which were supplied to tbl2asn are retained in that directory.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class PreviewGbfCommand extends BaseGbSubmissionGeneratorCommand<Void, PreviewGbfCommand.SequenceGbfResult, PreviewGbfResult> {
	
	public PreviewGbfCommand() {
		super();
		setGenerateGbf(true);
	}

	@CompleterClass
	public static class Completer extends GbSubmissionGeneratorCompleter {}
	
	/**
	 * summary for a single sequence.
	 */
	public static class SequenceGbfResult {
		
		private byte[] gbfBytes;
		private String sourceName;
		private String sequenceID;

		public SequenceGbfResult(String sourceName, String sequenceID, byte[] gbfBytes) {
			super();
			this.sourceName = sourceName;
			this.sequenceID = sequenceID;
			this.gbfBytes = gbfBytes;
		}
	}

	@Override
	protected Void initContext(ConsoleCommandContext consoleCmdContext) {
		return null;
	}

	@Override
	protected SequenceGbfResult intermediateResult(Void context, Tbl2AsnResult tbl2AsnResult) {
		return new SequenceGbfResult(tbl2AsnResult.getSourceName(), tbl2AsnResult.getSequenceID(), tbl2AsnResult.getGbfFileContent());
	}

	@Override
	protected PreviewGbfResult finalResult(List<SequenceGbfResult> intermediateResults) {
		SequenceGbfResult singleResult = intermediateResults.get(0);
		return new PreviewGbfResult(singleResult.sourceName, singleResult.sequenceID, singleResult.gbfBytes);
	}

}
