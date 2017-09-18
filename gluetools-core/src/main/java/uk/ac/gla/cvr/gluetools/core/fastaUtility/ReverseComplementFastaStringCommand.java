package uk.ac.gla.cvr.gluetools.core.fastaUtility;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(
		commandWords={"reverse-complement", "string"}, 
		description = "Reverse complement a FASTA string", 
		docoptUsages = { "-s <fastaString>" }, 
		docoptOptions = { 
				"-s <fastaString>, --fastaString <fastaString>  FASTA input string"
		},
		metaTags = {}	
)
public class ReverseComplementFastaStringCommand extends ModulePluginCommand<ReverseComplementFastaResult, FastaUtility>{

	private static final String FASTA_STRING = "fastaString";

	private String fastaString;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fastaString = PluginUtils.configureStringProperty(configElem, FASTA_STRING, true);
	}

	
	@Override
	protected ReverseComplementFastaResult execute(CommandContext cmdContext, FastaUtility fastaUtility) {
		DNASequence fastaNTSeq = FastaUtils.ntStringToSequence(fastaString);
		return new ReverseComplementFastaResult(FastaUtils.reverseComplement(fastaNTSeq.getSequenceAsString()));
	}

}
