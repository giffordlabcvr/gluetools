package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;


@CommandClass( 
	commandWords={"show", "data"}, 
	docoptUsages={""},
	docoptOptions={""},
	description="Show the original sequence data") 
public class ShowDataCommand extends SequenceModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Sequence sequence = lookupSequence(cmdContext);
		return SequenceFormat.valueOf(sequence.getFormat()).showDataResult(sequence.getData());
	}


}
