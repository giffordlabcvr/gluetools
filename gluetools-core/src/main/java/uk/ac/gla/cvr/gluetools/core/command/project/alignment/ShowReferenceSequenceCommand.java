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
public class ShowReferenceSequenceCommand extends AlignmentModeCommand<ShowReferenceSequenceCommand.ShowReferenceResult> {

	
	@Override
	public ShowReferenceResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		ReferenceSequence refSequence = alignment.getRefSequence();
		String refName = null;
		if(refSequence != null) {
			refName = refSequence.getName();
		}
		return new ShowReferenceResult(refName);
	}

	public static class ShowReferenceResult extends MapResult {

		public ShowReferenceResult(String referenceName) {
			super("showReferenceResult", mapBuilder().put("referenceName", referenceName));
		}

		public String getReferenceName() {
			return getDocumentReader().stringValue("referenceName");
		}
		
	}
}
