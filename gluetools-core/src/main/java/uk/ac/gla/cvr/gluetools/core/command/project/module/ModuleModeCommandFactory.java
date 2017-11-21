package uk.ac.gla.cvr.gluetools.core.command.project.module;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleCreatePropertyGroupCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleDeletePropertyGroupCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleSetPropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleShowPropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleUnsetPropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ModuleModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<ModuleModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ModuleModeCommandFactory.class, ModuleModeCommandFactory::new);

	@SuppressWarnings("rawtypes")
	private List<Class<? extends Command>> cmdClasses = null;
	private String moduleName = null;

	/* 
	 * two alternative constructors. The private one is only used to generate documentation for 
	 * general module commands. The public one is used for run time execution.
	 */
	private ModuleModeCommandFactory() {
	}	
	
	public ModuleModeCommandFactory(CommandContext cmdContext, String moduleName) {
		super();
		this.moduleName = moduleName;
		refreshCommandTree(cmdContext);
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void refreshCommandTree(CommandContext cmdContext) {
		if(moduleName != null) {
			Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(moduleName));
			@SuppressWarnings("rawtypes")
			List<Class<? extends Command>> providedCmdClasses = module.getProvidedCommandClasses(cmdContext);
			if(cmdClasses == null || !providedCmdClasses.equals(cmdClasses)) {
				cmdClasses = providedCmdClasses;
				resetCommandTree();
				populateCommandTree();
				cmdClasses.forEach(cmdClass -> 
				registerCommandClass((Class<? extends Command>) cmdClass));
			}

		}
	}

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		registerCommandClass(ModuleValidateCommand.class);
		registerCommandClass(ModuleShowConfigurationCommand.class);
		registerCommandClass(ModuleSetPropertyCommand.class);
		registerCommandClass(ModuleUnsetPropertyCommand.class);
		registerCommandClass(ModuleShowPropertyCommand.class);

		registerCommandClass(ModuleCreatePropertyGroupCommand.class);
		registerCommandClass(ModuleDeletePropertyGroupCommand.class);

		registerCommandClass(ModuleSaveConfigurationCommand.class);
		registerCommandClass(ModuleLoadConfigurationCommand.class);

		registerCommandClass(ExitCommand.class);
	}

	

}
