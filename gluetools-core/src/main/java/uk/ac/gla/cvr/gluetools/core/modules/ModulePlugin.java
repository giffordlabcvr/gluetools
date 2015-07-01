package uk.ac.gla.cvr.gluetools.core.modules;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public abstract class ModulePlugin<P extends ModulePlugin<P>> implements Plugin {
	
	
	private List<Class<? extends ModuleProvidedCommand<?>>> providedCmdClasses = 
			new ArrayList<Class<? extends ModuleProvidedCommand<?>>>();
	
	protected void addProvidedCmdClass(Class<? extends ModuleProvidedCommand<P>> providedCmdClass) {
		providedCmdClasses.add(providedCmdClass);
	}
	
	public List<Class<? extends ModuleProvidedCommand<?>>> getProvidedCommandClasses() {
		return providedCmdClasses;
	}
	

}
