package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;

@CommandClass(
		commandWords={"show", "reference"},
		docoptUsages={""},
		docoptOptions={},
		description="Return the name of the alignment's reference sequence"
)
public class ShowReferenceSequenceCommand extends AlignmentModeCommand {

	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Alignment alignment = GlueDataObject.lookup(cmdContext.getObjectContext(), 
				Alignment.class, Alignment.pkMap(getAlignmentName()), false);
		return new ShowReferenceResult(alignment.getRefSequence().getName());
	}

}
