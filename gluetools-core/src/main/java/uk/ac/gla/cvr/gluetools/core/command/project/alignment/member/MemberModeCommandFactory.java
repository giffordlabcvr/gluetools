package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
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
		registerCommandClass(MemberAddSegmentCommand.class);
		registerCommandClass(MemberRemoveSegmentCommand.class);
		registerCommandClass(MemberListSegmentCommand.class);
		
		registerCommandClass(MemberShowStatisticsCommand.class);
		registerCommandClass(MemberAminoAcidCommand.class);
		registerCommandClass(MemberCountAminoAcidCommand.class);
		registerCommandClass(MemberVariationScanCommand.class);

		registerCommandClass(MemberCreateFLocNoteCommand.class);
		registerCommandClass(MemberDeleteFLocNoteCommand.class);
		registerCommandClass(MemberListFLocNoteCommand.class);
		registerCommandClass(MemberFLocNoteCommand.class);
		
		registerCommandClass(RenderObjectCommand.class);

		ConfigurableObjectMode.registerConfigurableObjectCommands(this);
		
		registerCommandClass(ExitCommand.class);
	}
	

}
