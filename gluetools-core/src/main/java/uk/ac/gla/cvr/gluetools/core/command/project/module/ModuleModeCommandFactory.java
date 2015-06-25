package uk.ac.gla.cvr.gluetools.core.command.project.module;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ModuleModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<ModuleModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ModuleModeCommandFactory.class, ModuleModeCommandFactory::new);

	public ModuleModeCommandFactory() {
		super();
		
		registerCommandClass(ShowConfigCommand.class);
		registerCommandClass(RunModuleCommand.class);

		
		registerCommandClass(ExitCommand.class);
	}
	

}
