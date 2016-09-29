package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@CommandClass(
		commandWords={"show", "reference"},
		docoptUsages={""},
		docoptOptions={},
		description="Return the name of the alignment's reference sequence"
)
public class AlignmentShowReferenceSequenceCommand extends AlignmentModeCommand<AlignmentShowReferenceSequenceCommand.ShowReferenceResult> {

	
	@Override
	public ShowReferenceResult execute(CommandContext cmdContext) {
		return new ShowReferenceResult(lookupAlignment(cmdContext).getRefSequence());
	}

	public static class ShowReferenceResult extends MapResult {

		public ShowReferenceResult(ReferenceSequence refSequence) {
			super("showReferenceResult", mapBuilder()
					.put("referenceName", refSequence != null ? refSequence.getName() : null)
					.put("referenceRenderedName", refSequence != null ? refSequence.getRenderedName() : null)
				);
		}

		public String getReferenceName() {
			return getCommandDocument().getString("referenceName");
		}
		
	}
}
