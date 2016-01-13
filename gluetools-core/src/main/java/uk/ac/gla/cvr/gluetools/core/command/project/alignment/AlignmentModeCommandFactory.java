package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class AlignmentModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<AlignmentModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(AlignmentModeCommandFactory.class, AlignmentModeCommandFactory::new);

	private AlignmentModeCommandFactory() {
	}	
	
	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		registerCommandClass(AddMemberCommand.class);
		registerCommandClass(RemoveMemberCommand.class);
		registerCommandClass(ListMemberCommand.class);
		registerCommandClass(MemberCommand.class);
		registerCommandClass(ShowReferenceSequenceCommand.class);
		registerCommandClass(AlignmentShowStatisticsCommand.class);

		registerCommandClass(AlignmentSetParentCommand.class);
		registerCommandClass(AlignmentUnsetParentCommand.class);
		registerCommandClass(AlignmentListChildrenCommand.class);
		registerCommandClass(AlignmentShowParentCommand.class);
		registerCommandClass(AlignmentShowAncestorsCommand.class);

		registerCommandClass(AlignmentExtractChildCommand.class);
		registerCommandClass(AlignmentDemoteMemberCommand.class);
		
		registerCommandClass(ExitCommand.class);
	}
	

}
