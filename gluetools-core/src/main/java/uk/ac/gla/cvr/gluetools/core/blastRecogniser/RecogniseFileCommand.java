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
package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@CommandClass(
		commandWords={"recognise", "file"}, 
		description = "Apply recognition to sequences in a FASTA file", 
		docoptUsages = { "-i <inputFile>" },
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>     FASTA file path",
		},
		metaTags = {CmdMeta.consoleOnly}	
)
public class RecogniseFileCommand extends ModulePluginCommand<BlastSequenceRecogniserResult, BlastSequenceRecogniser> 
	implements ProvidedProjectModeCommand {

	public final static String INPUT_FILE = "inputFile";
	
	private String inputFile;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.inputFile = PluginUtils.configureStringProperty(configElem, INPUT_FILE, true);
	}

	@Override
	protected BlastSequenceRecogniserResult execute(CommandContext cmdContext, BlastSequenceRecogniser blastSequenceRecogniser) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		byte[] fastaBytes = consoleCommandContext.loadBytes(inputFile);
		FastaUtils.normalizeFastaBytes(cmdContext, fastaBytes);
		Map<String, DNASequence> querySequenceMap = FastaUtils.parseFasta(fastaBytes);
		Map<String, List<RecognitionCategoryResult>> queryIdToCatResult = blastSequenceRecogniser.recognise(cmdContext, querySequenceMap);
		List<BlastSequenceRecogniserResultRow> resultRows = BlastSequenceRecogniserResultRow.rowsFromMap(queryIdToCatResult);
		return new BlastSequenceRecogniserResult(resultRows);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("inputFile", false);
		}
		
	}


}
