package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class MemberModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<MemberModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(MemberModeCommandFactory.class, MemberModeCommandFactory::new);

	private MemberModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		registerCommandClass(AddAlignedSegmentCommand.class);
		registerCommandClass(RemoveAlignedSegmentCommand.class);
		registerCommandClass(ListAlignedSegmentCommand.class);
		
		registerCommandClass(MemberShowStatisticsCommand.class);
		registerCommandClass(MemberAminoAcidsCommand.class);
		
		registerCommandClass(ExitCommand.class);
	}
	

}
