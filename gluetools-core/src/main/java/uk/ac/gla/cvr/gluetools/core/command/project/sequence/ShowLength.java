package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;


@CommandClass( 
	commandWords={"show", "length"}, 
	docoptUsages={""},
	description="Show the length of the sequence in nucleotides") 
public class ShowLength extends SequenceModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Sequence sequence = lookupSequence(cmdContext);
		return new LengthResult(sequence.getNucleotides().length());
	}


}
