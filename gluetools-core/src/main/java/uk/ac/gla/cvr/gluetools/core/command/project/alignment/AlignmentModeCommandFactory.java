package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
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

		setCmdGroup(new CommandGroup("members", "Commands for managing alignment members", 49, false));
		registerCommandClass(AlignmentAddMemberCommand.class);
		registerCommandClass(AlignmentRemoveMemberCommand.class);
		registerCommandClass(AlignmentListMemberCommand.class);
		registerCommandClass(AlignmentWebListMemberCommand.class);
		registerCommandClass(AlignmentCountMemberCommand.class);

		setCmdGroup(new CommandGroup("members", "Commands for managing alignment member segments", 49, false));
		registerCommandClass(AlignmentDeriveSegmentsCommand.class);

		setCmdGroup(new CommandGroup("properties", "Commands for querying properties of the alignment", 49, false));
		registerCommandClass(AlignmentShowReferenceSequenceCommand.class);


		setCmdGroup(new CommandGroup("parent", "Commands for managing alignment parent-child relationships", 50, false));
		registerCommandClass(AlignmentSetParentCommand.class);
		registerCommandClass(AlignmentUnsetParentCommand.class);
		registerCommandClass(AlignmentShowParentCommand.class);
		registerCommandClass(AlignmentListChildrenCommand.class);
		registerCommandClass(AlignmentListDescendentCommand.class);
		registerCommandClass(AlignmentShowAncestorsCommand.class);
		registerCommandClass(AlignmentDescendentTreeCommand.class);
		registerCommandClass(AlignmentExtractChildCommand.class);
		registerCommandClass(AlignmentDemoteMemberCommand.class);

		ConfigurableObjectMode.registerConfigurableObjectCommands(this);

		setCmdGroup(new CommandGroup("analysis", "Commands performing basic analysis based on the stored homologies", 51, false));
		registerCommandClass(AlignmentAminoAcidFrequencyCommand.class);
		registerCommandClass(AlignmentVariationFrequencyCommand.class);
		registerCommandClass(AlignmentVariationMemberScanCommand.class);
		registerCommandClass(AlignmentShowStatisticsCommand.class);
		registerCommandClass(AlignmentShowMemberFeatureCoverageCommand.class);
		registerCommandClass(AlignmentShowFeaturePresenceCommand.class);
		registerCommandClass(AlignmentScoreCoverageCommand.class);
		
		setCmdGroup(CommandGroup.RENDERING);
		registerCommandClass(RenderObjectCommand.class);

		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(MemberCommand.class);
		registerCommandClass(ExitCommand.class);
	}
	

}
