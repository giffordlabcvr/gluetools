/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.project.module;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroupRegistry;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.ReturnToProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleCreatePropertyGroupCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleDeletePropertyGroupCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleSetPropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleShowPropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleUnsetPropertyCommand;
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
			CommandGroupRegistry commandGroupRegistry = module.getCommandGroupRegistry(cmdContext);
			@SuppressWarnings("rawtypes")
			List<Class<? extends Command>> providedCmdClasses = module.getProvidedCommandClasses(cmdContext);
			if(cmdClasses == null || !providedCmdClasses.equals(cmdClasses)) {
				cmdClasses = providedCmdClasses;
				resetCommandTree();
				populateCommandTree();
				cmdClasses.forEach(cmdClass -> {
					setCmdGroup(commandGroupRegistry.getCmdGroupForCmdClass(cmdClass));
					registerCommandClass((Class<? extends Command>) cmdClass);
				});
				setCmdGroup(null);
			}

		}
	}

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		setCmdGroup(CommandGroup.VALIDATION);
		registerCommandClass(ModuleValidateCommand.class);

		setCmdGroup(new CommandGroup("configuration", "General commands for managing the module configuration", 49, false));
		registerCommandClass(ModuleSaveConfigurationCommand.class);
		registerCommandClass(ModuleLoadConfigurationCommand.class);
		registerCommandClass(ModuleShowConfigurationCommand.class);

		setCmdGroup(new CommandGroup("simple-properties", "Commands for managing simple properties within the configuration document", 51, false));
		registerCommandClass(ModuleSetPropertyCommand.class);
		registerCommandClass(ModuleUnsetPropertyCommand.class);
		registerCommandClass(ModuleShowPropertyCommand.class);

		setCmdGroup(new CommandGroup("property-groups", "Commands for managing property groups within the configuration document", 52, false));
		registerCommandClass(ModuleCreatePropertyGroupCommand.class);
		registerCommandClass(ModuleDeletePropertyGroupCommand.class);

		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(ExitCommand.class);
		registerCommandClass(ReturnToProjectModeCommand.class);

	}

	

}
