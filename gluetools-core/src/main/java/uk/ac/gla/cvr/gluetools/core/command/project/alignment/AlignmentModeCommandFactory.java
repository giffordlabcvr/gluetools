package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class AlignmentModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<AlignmentModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(AlignmentModeCommandFactory.class, AlignmentModeCommandFactory::new);

	private AlignmentModeCommandFactory() {
	}	
	
	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		registerCommandClass(AlignmentAddMemberCommand.class);
		registerCommandClass(AlignmentRemoveMemberCommand.class);
		registerCommandClass(AlignmentListMemberCommand.class);
		registerCommandClass(MemberCommand.class);
		registerCommandClass(AlignmentShowReferenceSequenceCommand.class);
		registerCommandClass(AlignmentShowStatisticsCommand.class);

		registerCommandClass(AlignmentSetParentCommand.class);
		registerCommandClass(AlignmentUnsetParentCommand.class);
		registerCommandClass(AlignmentShowParentCommand.class);

		ConfigurableObjectMode.registerConfigurableObjectCommands(this);

		registerCommandClass(AlignmentListChildrenCommand.class);
		registerCommandClass(AlignmentListDescendentCommand.class);
		registerCommandClass(AlignmentShowAncestorsCommand.class);

		registerCommandClass(AlignmentExtractChildCommand.class);
		registerCommandClass(AlignmentDemoteMemberCommand.class);
		
		registerCommandClass(AlignmentDeriveSegmentsCommand.class);
		
		registerCommandClass(AlignmentAminoAcidFrequencyCommand.class);
		registerCommandClass(AlignmentVariationFrequencyCommand.class);

		registerCommandClass(RenderObjectCommand.class);

		registerCommandClass(ExitCommand.class);
	}
	

}
