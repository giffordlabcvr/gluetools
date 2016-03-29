package uk.ac.gla.cvr.gluetools.core.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleLoadConfigurationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleSaveConfigurationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleSetSimplePropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleShowConfigurationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleShowSimplePropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleUnsetSimplePropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleValidateCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class ModulePlugin<P extends ModulePlugin<P>> implements Plugin {
	
	public static final String LOG_LEVEL = "logLevel";
	
	private Level moduleLogLevel = null;
	
	private List<String> simplePropertyNames = new ArrayList<String>();
	
	public ModulePlugin() {
		super();
		addModuleDocumentCmdClass(ModuleValidateCommand.class);
		addModuleDocumentCmdClass(ModuleShowConfigurationCommand.class);
		addModuleDocumentCmdClass(ModuleSetSimplePropertyCommand.class);
		addModuleDocumentCmdClass(ModuleUnsetSimplePropertyCommand.class);
		addModuleDocumentCmdClass(ModuleShowSimplePropertyCommand.class);
		addModuleDocumentCmdClass(ModuleSaveConfigurationCommand.class);
		addModuleDocumentCmdClass(ModuleLoadConfigurationCommand.class);
		addSimplePropertyName(LOG_LEVEL);
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		String logLevelString = PluginUtils.configureStringProperty(configElem, LOG_LEVEL, 
				Arrays.asList(GlueLogger.ALL_LOG_LEVELS), false);
		if(logLevelString != null) {
			this.moduleLogLevel = Level.parse(logLevelString);
		}
	}

	protected void addSimplePropertyName(String simplePropertyName) {
		this.simplePropertyNames.add(simplePropertyName);
	}
	
	public List<String> getSimplePropertyNames() {
		return simplePropertyNames;
	}

	@SuppressWarnings("rawtypes")
	private List<Class<? extends Command>> providedCmdClasses = 
			new ArrayList<Class<? extends Command>>();
	
	protected void addModulePluginCmdClass(Class<? extends ModulePluginCommand<?, P>> providedCmdClass) {
		providedCmdClasses.add(providedCmdClass);
	}

	protected void addModuleDocumentCmdClass(Class<? extends ModuleDocumentCommand<?>> providedCmdClass) {
		providedCmdClasses.add(providedCmdClass);
	}

	
	@SuppressWarnings("rawtypes")
	public List<Class<? extends Command>> getProvidedCommandClasses() {
		return providedCmdClasses;
	}
	
	protected Project getProject(CommandContext cmdContext) {
		return ((ProjectMode) cmdContext.peekCommandMode()).getProject();
	}

	public void log(String message) {
		log(Level.FINEST, message);
	}
	public void log(Level msgLogLevel, String message) {
		if(moduleLogLevel == null || 
				msgLogLevel.intValue() >= moduleLogLevel.intValue()) {
			GlueLogger.log(msgLogLevel, message);
		} 
	}

	public void validate(CommandContext cmdContext) {
	}
	
	
}
