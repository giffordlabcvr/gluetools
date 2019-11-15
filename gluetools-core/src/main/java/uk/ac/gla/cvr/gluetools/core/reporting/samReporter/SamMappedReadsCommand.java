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
package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.io.IOException;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

@CommandClass(
		commandWords={"mapped-reads"}, 
		description = "Basic statistics on mapped reads in a SAM/BAM file", 
		docoptUsages = { "-i <fileName> [-s <samRefName>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                    SAM/BAM input file",
				"-s <samRefName>, --samRefName <samRefName>              Specific SAM ref seq"
		},
		furtherHelp = 
			"This generates statistics for reads mapped to a specific reference in a SAM/BAM file, vs those unmapped or "+
			"mapped to some other reference. "+
			"The <sanmRefName> specifies a reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
			"file only names a single reference sequence, if not an error is thrown.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamMappedReadsCommand extends BaseSamReporterCommand<SamMappedReadsResult> 
	implements ProvidedProjectModeCommand {


	@Override
	protected SamMappedReadsResult execute(CommandContext cmdContext, SamReporter samReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		SamRefInfo samRefInfo = getSamRefInfo(consoleCmdContext, samReporter);
		ValidationStringency validationStringency = samReporter.getSamReaderValidationStringency();
		
		ResultStats resultStats = new ResultStats();

		try(SamReader samReader = SamUtils.newSamReader(consoleCmdContext, getFileName(), validationStringency)) {
			SamUtils.ReferenceBasedRecordFilter recordFilter = new SamUtils.ReferenceBasedRecordFilter(samReader, getFileName(), getSuppliedSamRefName());
			SamUtils.iterateOverSamReader(samReader, samRecord -> {
				if(recordFilter.recordPasses(samRecord)) {
					resultStats.mappedToReference++;
					if(samRecord.getReadNegativeStrandFlag()) {
						resultStats.reverseMappedToReference++;
					} else {
						resultStats.fwdMappedToReference++;
					}
				} else {
					resultStats.notMappedToReference++;
				}
				resultStats.totalReads++;
				if(resultStats.totalReads % 10000 == 0) {
					GlueLogger.getGlueLogger().finest("Processed "+resultStats.totalReads+" reads");
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
		}
		GlueLogger.getGlueLogger().finest("Processed "+resultStats.totalReads+" reads");

		return new SamMappedReadsResult(samRefInfo, 
					resultStats.totalReads,
					resultStats.mappedToReference, resultStats.fwdMappedToReference, 
					resultStats.reverseMappedToReference, resultStats.notMappedToReference);

	}

	private class ResultStats{ 
		private int totalReads = 0;
		private int mappedToReference = 0;
		private int fwdMappedToReference = 0;
		private int reverseMappedToReference = 0;
		private int notMappedToReference = 0;
	}
	
	@CompleterClass
	public static class Completer extends BaseSamReporterCommand.Completer {
	}

}
