package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;


@CommandClass( 
	commandWords={"show", "original-data"}, 
	docoptUsages={""},
	docoptOptions={},
	description="Show the original sequence data",
	furtherHelp="Returns the data from which the sequence object was created, in its original format") 
public class ShowOriginalDataCommand extends SequenceModeCommand<OriginalDataResult> {

	@Override
	public OriginalDataResult execute(CommandContext cmdContext) {
		Sequence sequence = lookupSequence(cmdContext);
		return new OriginalDataResult(SequenceFormat.valueOf(sequence.getFormat()),
				sequence.getOriginalData());
	}


}
