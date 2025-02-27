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

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor.SamReporterPreprocessorSession;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor.SamReporterPreprocessorSession.ReferenceDistance;

@CommandClass(
		commandWords={"target-reference"}, 
		description = "Compute the closest target reference for a SAM/BAM file", 
		docoptUsages = { "-i <fileName> [-n <samRefSense>] [-s <samRefName>]" },
				docoptOptions = { 
						"-i <fileName>, --fileName <fileName>                    SAM/BAM input file",
						"-n <samRefSense>, --samRefSense <samRefSense>           SAM ref seq sense",
						"-s <samRefName>, --samRefName <samRefName>              Specific SAM ref seq",
				},
				furtherHelp = 
					"This computes the best target reference for a SAM/BAM file, using the maxLikelihoodPlacer module specified in the "+
					"samReporter module configuration. If <samRefName> is supplied, the reads are limited to those which are aligned to the "+
					"specified reference sequence named in the SAM/BAM file. If <samRefName> is omitted, it is assumed that the input "+
					"file only names a single reference sequence.\n"+
					"The <samRefSense> may be FORWARD or REVERSE_COMPLEMENT, indicating the presumed sense of the SAM reference, relative to the GLUE references.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class SamTargetReferenceCommand extends ExtendedSamReporterCommand<SamTargetReferenceResult> implements ProvidedProjectModeCommand {

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
	}

	@Override
	protected SamTargetReferenceResult execute(CommandContext cmdContext, SamReporter samReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		String samFileName = getFileName();
		try(SamReporterPreprocessorSession samReporterPreprocessorSession = SamReporterPreprocessor.getPreprocessorSession(consoleCmdContext, samFileName, samReporter)) {
			ReferenceDistance refDistance = samReporterPreprocessorSession.getTargetRefDistanceBasedOnPlacer(consoleCmdContext, samReporter, this);
			return new SamTargetReferenceResult(refDistance.getTargetRef().getName(), refDistance.getDistance());
		}

	}
	
	@CompleterClass
	public static class Completer extends ExtendedSamReporterCommand.Completer {
		public Completer() {
			super();
		}
	}
}
