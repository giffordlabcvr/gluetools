package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"show", "nucleotides"}, 
	docoptUsages={"[-b <beginIndex>] [-e <endIndex>]"},
	docoptOptions={
		"-b <idx>, --beginIndex <idx>  Start index of the subsequence (from 1, inclusive)",
		"-e <idx>, --endIndex <idx>    End index of the subsequence (inclusive)"},
	description="Show nucleotides from the sequence",
	furtherHelp="A subsequence is returned using nucleotide codes in FASTA format. If the beginIndex is omitted the subsequence starts at the beginning of the sequence. Similarly if the endIndex is omitted, the subsequence starts at the end of the sequence.") 
public class ShowNucleotides extends SequenceModeCommand {

	private Integer beginIndex;
	private Optional<Integer> endIndex;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		beginIndex = PluginUtils.configureIntProperty(configElem, "beginIndex", 1);
		endIndex = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "endIndex", false));
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Sequence sequence = lookupSequence(cmdContext);
		String nucleotides = sequence.getNucleotides();
		int end = endIndex.orElse(nucleotides.length());
		return new NucleotidesResult(beginIndex, end, nucleotides.subSequence(beginIndex-1, end).toString());
	}


}
