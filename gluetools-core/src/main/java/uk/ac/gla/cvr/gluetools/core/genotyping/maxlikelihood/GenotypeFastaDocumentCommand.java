package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.io.File;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(
		commandWords={"genotype", "fasta-document"}, 
		description = "Genotype sequences in a FASTA command document object", 
		docoptUsages = { },
		docCategory = "Type-specific module commands",
		furtherHelp = "If supplied, <dataDir> must either not exist or be an empty directory",
		metaTags = {CmdMeta.inputIsComplex}	
)
public class GenotypeFastaDocumentCommand extends AbstractGenotypeCommand {

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
	protected GenotypeCommandResult execute(CommandContext cmdContext, MaxLikelihoodGenotyper maxLikelihoodGenotyper) {
		Map<String, DNASequence> querySequenceMap = FastaUtils.commandDocumentToNucleotideFastaMap(cmdDocument);
		File dataDirFile = CommandUtils.ensureDataDir(cmdContext, dataDir);
		Map<String, QueryGenotypingResult> genotypeResults = maxLikelihoodGenotyper.genotype(cmdContext, querySequenceMap, dataDirFile);
		return formResult(maxLikelihoodGenotyper, genotypeResults);
	}
}
