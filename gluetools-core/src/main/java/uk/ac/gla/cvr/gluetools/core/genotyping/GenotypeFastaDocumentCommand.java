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
package uk.ac.gla.cvr.gluetools.core.genotyping;

import java.io.File;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

public abstract class GenotypeFastaDocumentCommand<P extends BaseGenotyper<P>> extends AbstractGenotypeCommand<P> {

	public final static String FASTA_COMMAND_DOCUMENT = "fastaCommandDocument";
	public final static String DATA_DIR = "dataDir";
	
	private CommandDocument cmdDocument;
	private String dataDir;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.cmdDocument = PluginUtils.configureCommandDocumentProperty(configElem, FASTA_COMMAND_DOCUMENT, true);
		this.dataDir = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);
	}
	
	@Override
	protected CommandResult execute(CommandContext cmdContext, P maxLikelihoodGenotyper) {
		Map<String, DNASequence> querySequenceMap = FastaUtils.commandDocumentToNucleotideFastaMap(cmdDocument);
		File dataDirFile = CommandUtils.ensureDataDir(cmdContext, dataDir);
		Map<String, QueryGenotypingResult> genotypeResults = maxLikelihoodGenotyper.genotype(cmdContext, querySequenceMap, dataDirFile);
		return formResult(maxLikelihoodGenotyper, genotypeResults);
	}
}
