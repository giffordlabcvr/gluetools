package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

@CommandClass(
		commandWords={"show", "creation", "time"},
		docoptUsages={""},
		docoptOptions={},
		description="Return the creation time"
)
public class ReferenceShowCreationTimeCommand extends ReferenceSequenceModeCommand<ReferenceShowCreationTimeCommand.ReferenceShowCreationTimeResult> {

	
	@Override
	public ReferenceShowCreationTimeResult execute(CommandContext cmdContext) {
		return new ReferenceShowCreationTimeResult(Long.toString(lookupRefSeq(cmdContext).getCreationTime()));
	}

	public static class ReferenceShowCreationTimeResult extends MapResult {

		public ReferenceShowCreationTimeResult(String creationTime) {
			super("referenceShowCreationTimeResult", mapBuilder()
				.put("creationTime", creationTime));
		}

		public long getCreationTime() {
			return Long.parseLong(getCommandDocument().getString("creationTime"));
		}

		
	}

	
}
