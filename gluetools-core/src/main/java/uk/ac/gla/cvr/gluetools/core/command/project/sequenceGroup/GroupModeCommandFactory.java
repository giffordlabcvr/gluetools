package uk.ac.gla.cvr.gluetools.core.command.project.sequenceGroup;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class GroupModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<GroupModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(GroupModeCommandFactory.class, GroupModeCommandFactory::new);

	private GroupModeCommandFactory() {
	}	
	
	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		registerCommandClass(AddMemberCommand.class);
		registerCommandClass(RemoveMemberCommand.class);
		registerCommandClass(ListMemberCommand.class);
		registerCommandClass(GroupShowLastUpdateTimeCommand.class);
		
		registerCommandClass(ExitCommand.class);
	}
	

}
