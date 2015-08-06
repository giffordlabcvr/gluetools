package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@CommandClass(
		commandWords={"show", "sequence"},
		docoptUsages={""},
		docoptOptions={},
		description="Return the source/sequenceID sequence"
)
public class ShowSequenceCommand extends ReferenceSequenceModeCommand {

	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ReferenceSequence refSeq = GlueDataObject.lookup(cmdContext.getObjectContext(), 
				ReferenceSequence.class, ReferenceSequence.pkMap(getRefSeqName()), false);
		return new ShowSequenceResult(refSeq.getSequence().getSource().getName(), refSeq.getSequence().getSequenceID());
	}

}
