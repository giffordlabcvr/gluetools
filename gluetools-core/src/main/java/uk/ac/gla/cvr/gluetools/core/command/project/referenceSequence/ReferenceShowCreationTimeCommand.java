package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@CommandClass(
		commandWords={"show", "creation", "time"},
		docoptUsages={""},
		docoptOptions={},
		description="Return the creation time"
)
public class ReferenceShowCreationTimeCommand extends ReferenceSequenceModeCommand<ReferenceShowCreationTimeCommand.ReferenceShowCreationTimeResult> {

	
	@Override
	public ReferenceShowCreationTimeResult execute(CommandContext cmdContext) {
		ReferenceSequence refSeq = GlueDataObject.lookup(cmdContext.getObjectContext(), 
				ReferenceSequence.class, ReferenceSequence.pkMap(getRefSeqName()), false);
		return new ReferenceShowCreationTimeResult(Long.toString(refSeq.getCreationTime()));
	}

	public static class ReferenceShowCreationTimeResult extends MapResult {

		public ReferenceShowCreationTimeResult(String creationTime) {
			super("showSequenceResult", mapBuilder()
				.put("creationTime", creationTime));
		}

		public long getCreationTime() {
			return Long.parseLong(getDocumentReader().stringValue("creationTime"));
		}

		
	}

	
}
