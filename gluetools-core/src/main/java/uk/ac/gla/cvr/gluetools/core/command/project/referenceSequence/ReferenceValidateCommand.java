package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@CommandClass( 
		commandWords={"validate"}, 
		docoptUsages={""},
		docoptOptions={},
		metaTags={},
		description="Validate that a reference sequence is correctly defined.", 
		furtherHelp="Also validates any feature locations for this reference sequence.") 
public class ReferenceValidateCommand extends ReferenceSequenceModeCommand<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		ReferenceSequence refSeq = lookupRefSeq(cmdContext);
		refSeq.validate(cmdContext);
		return new OkResult();
	}
	
}

