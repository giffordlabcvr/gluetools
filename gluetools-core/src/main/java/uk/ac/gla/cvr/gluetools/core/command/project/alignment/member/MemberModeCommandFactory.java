package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class MemberModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<MemberModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(MemberModeCommandFactory.class, MemberModeCommandFactory::new);

	private MemberModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		setCmdGroup(new CommandGroup("segments", "Commands for managing aligned segments", 49, false));
		registerCommandClass(MemberAddSegmentCommand.class);
		registerCommandClass(MemberRemoveSegmentCommand.class);
		registerCommandClass(MemberListSegmentCommand.class);
		
		setCmdGroup(new CommandGroup("analysis", "Commands performing basic analysis based on the stored homologies", 50, false));
		registerCommandClass(MemberShowStatisticsCommand.class);
		registerCommandClass(MemberAminoAcidCommand.class);
		registerCommandClass(MemberCountAminoAcidCommand.class);
		registerCommandClass(MemberVariationScanCommand.class);

		setCmdGroup(new CommandGroup("member-floc-notes", "Commands for managing member-feature-location notes", 51, false));
		registerCommandClass(MemberCreateFLocNoteCommand.class);
		registerCommandClass(MemberDeleteFLocNoteCommand.class);
		registerCommandClass(MemberListFLocNoteCommand.class);
		
		setCmdGroup(CommandGroup.RENDERING);
		registerCommandClass(RenderObjectCommand.class);

		ConfigurableObjectMode.registerConfigurableObjectCommands(this);
		
		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(ExitCommand.class);
		registerCommandClass(MemberFLocNoteCommand.class);
	}
	

}
