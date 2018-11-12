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

import java.io.File;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;


@CommandClass(
		commandWords={"generate-sqn"}, 
		description = "Generate .sqn submission files from a set of stored GLUE sequences", 
		docoptUsages = { "(-w <whereClause> | -a | -s <sourceName> <sequenceID>) -t <templateFile> [-o <outputDir>] [-d <dataDir>]" },
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>     Qualify the sequence set",
				"-a, --allSequences                                All sequences in the project",
				"-s, --specificSequence                            Specific <sourceName> + <sequenceID>",
				"-t <templateFile>, --templateFile <templateFile>  Template .sbt file",
				"-o <outputDir>, --outputDir <outputDir>           Directory for .sqn files",
				"-d <dataDir>, --dataDir <dataDir>                 Directory for intermediate files",
		},
		furtherHelp = "This command uses tbl2asn as a subroutine to generate .sqn files, for GenBank submssion. "+
		"If <outputDir> is omitted, the files are written to the current load/save path. If <outputDir> does not "+
		"exist, it is created. If <dataDir> is supplied, it is created if it does not exist and the the intermediate "+
		"files which were supplied to tbl2asn are retained in that directory.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class GenerateSqnCommand extends FileGeneratingGbSubmissionGeneratorCommand<GenerateSqnCommand.SequenceSqnResult, GenerateSqnResult> {
	
	@CompleterClass
	public static class Completer extends FileGeneratingGbSubmissionGeneratorCompleter {}
	
	/**
	 * summary for a single sequence.
	 */
	public static class SequenceSqnResult {
		
		private String sourceName;
		private String sequenceID;
		private String filePath;
		private SequenceSqnResult(String sourceName, String sequenceID,
				String filePath) {
			super();
			this.sourceName = sourceName;
			this.sequenceID = sequenceID;
			this.filePath = filePath;
		}
		public String getSourceName() {
			return sourceName;
		}
		public String getSequenceID() {
			return sequenceID;
		}
		public String getFilePath() {
			return filePath;
		}
	}

	@Override
	protected SequenceSqnResult intermediateResult(File outputDirFile, Tbl2AsnResult tbl2AsnResult) {
		File sqnFile = new File(outputDirFile, tbl2AsnResult.getId()+".sqn");
		ConsoleCommandContext.saveBytesToFile(sqnFile, tbl2AsnResult.getSqnFileContent());
		return new SequenceSqnResult(tbl2AsnResult.getSourceName(), tbl2AsnResult.getSequenceID(), sqnFile.getAbsolutePath());
	}

	@Override
	protected GenerateSqnResult finalResult(List<SequenceSqnResult> intermediateResults) {
		return new GenerateSqnResult(intermediateResults);
	}

}
