package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class AlignmentModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<AlignmentModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(AlignmentModeCommandFactory.class, AlignmentModeCommandFactory::new);

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		registerCommandClass(AddMemberCommand.class);
		registerCommandClass(RemoveMemberCommand.class);
		registerCommandClass(ListMemberCommand.class);
		registerCommandClass(MemberCommand.class);

		
		registerCommandClass(ExitCommand.class);
	}
	

}