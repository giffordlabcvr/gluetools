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
package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.io.File;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentUtils;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer.PlacerResultInternal;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"place", "file"}, 
		description = "Place sequences from a file into a phylogeny", 
		docoptUsages = { "-i <inputFile> [-d <dataDir>] -o <outputFile>" },
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>     FASTA file path",
				"-o <outputFile>, --outputFile <outputFile>  Output file path for placement results",
				"-d <dataDir>, --dataDir <dataDir>           Save algorithmic data in this directory",
		},
		furtherHelp = "If supplied, <dataDir> must either not exist or be an empty directory",
		metaTags = {CmdMeta.consoleOnly}	
)
public class PlaceFileCommand extends AbstractPlaceCommand<OkResult> {

	public final static String INPUT_FILE = "inputFile";
	public final static String OUTPUT_FILE = "outputFile";

	
	private String inputFile;
	private String outputFile;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.inputFile = PluginUtils.configureStringProperty(configElem, INPUT_FILE, true);
		this.outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);

	}

	@Override
	protected OkResult execute(CommandContext cmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		byte[] fastaBytes = consoleCommandContext.loadBytes(inputFile);
		FastaUtils.normalizeFastaBytes(cmdContext, fastaBytes);
		Map<String, DNASequence> querySequenceMap = FastaUtils.parseFasta(fastaBytes);
		File dataDirFile = CommandUtils.ensureDataDir(consoleCommandContext, getDataDir());
		PlacerResultInternal placerResultInternal = maxLikelihoodPlacer.place(consoleCommandContext, querySequenceMap, dataDirFile);
		CommandDocument placerResultCmdDocument = PojoDocumentUtils.pojoToCommandDocument(placerResultInternal.toPojoResult());
		Document placerResultXmlDoc = CommandDocumentXmlUtils.commandDocumentToXmlDocument(placerResultCmdDocument);
		byte[] placerResultXmlBytes = GlueXmlUtils.prettyPrint(placerResultXmlDoc);
		consoleCommandContext.saveBytes(outputFile, placerResultXmlBytes);
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends AbstractPlaceCommandCompleter {
		public Completer() {
			super();
			registerPathLookup("inputFile", false);
		}
	}

	
}
