package uk.ac.gla.cvr.gluetools.core.command.project.sequenceGroup;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

@CommandClass(
		commandWords={"show", "last", "update", "time"},
		docoptUsages={""},
		docoptOptions={},
		description="Return the last update time"
)
public class GroupShowLastUpdateTimeCommand extends GroupModeCommand<GroupShowLastUpdateTimeCommand.GroupShowLastUpdateTimeResult> {

	
	@Override
	public GroupShowLastUpdateTimeResult execute(CommandContext cmdContext) {
		return new GroupShowLastUpdateTimeResult(Long.toString(lookupGroup(cmdContext).getLastUpdateTime()));
	}

	public static class GroupShowLastUpdateTimeResult extends MapResult {

		public GroupShowLastUpdateTimeResult(String lastUpdateTime) {
			super("groupShowLastUpdateTimeResult", mapBuilder()
				.put("lastUpdateTime", lastUpdateTime));
		}

		public long getLastUpdateTime() {
			return Long.parseLong(getDocumentReader().stringValue("lastUpdateTime"));
		}

		
	}

	
}
