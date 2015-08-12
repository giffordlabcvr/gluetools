package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@CommandClass(
		commandWords={"show", "creation", "time"},
		docoptUsages={""},
		docoptOptions={},
		description="Return the creation time"
)
public class ShowCreationTimeCommand extends ReferenceSequenceModeCommand<ShowCreationTimeResult> {

	
	@Override
	public ShowCreationTimeResult execute(CommandContext cmdContext) {
		ReferenceSequence refSeq = GlueDataObject.lookup(cmdContext.getObjectContext(), 
				ReferenceSequence.class, ReferenceSequence.pkMap(getRefSeqName()), false);
		return new ShowCreationTimeResult(Long.toString(refSeq.getCreationTime()));
	}

}
