package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.varAlmtNote;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class VarAlmtNoteModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<VarAlmtNoteModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(VarAlmtNoteModeCommandFactory.class, VarAlmtNoteModeCommandFactory::new);

	private VarAlmtNoteModeCommandFactory() {
	}	
	
	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		registerCommandClass(VarAlmtNoteSetFieldCommand.class);
		registerCommandClass(VarAlmtNoteShowPropertyCommand.class);
		registerCommandClass(VarAlmtNoteUnsetFieldCommand.class);
		
		registerCommandClass(ExitCommand.class);
	}
	

}
