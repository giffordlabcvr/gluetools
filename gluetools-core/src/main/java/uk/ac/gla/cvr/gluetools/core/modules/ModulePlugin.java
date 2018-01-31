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
package uk.ac.gla.cvr.gluetools.core.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter.VariableInstantiator;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroupRegistry;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException;
import uk.ac.gla.cvr.gluetools.core.datamodel.moduleResource.ModuleResource;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class ModulePlugin<P extends ModulePlugin<P>> implements Plugin {
	
	public static final String LOG_LEVEL = "logLevel";
	
	private Level moduleLogLevel = null;
	
	private PropertyGroup rootPropertyGroup = new PropertyGroup();
	
	private Set<String> resourceNames = new LinkedHashSet<String>();
	
	private CommandGroupRegistry commandGroupRegistry = new CommandGroupRegistry();
	
	private String moduleName;
	
	public ModulePlugin() {
		super();
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

	// call this during configure to register a resource name
	// the resource will then get loaded during loadResources()
	protected void registerResourceName(String resourceName) {
		resourceNames.add(resourceName);
	}
	
	protected void addSimplePropertyName(String simplePropertyName) {
		getRootPropertyGroup().addPropertyName(simplePropertyName);
	}

	protected void addSimplePropertyName(String simplePropertyName, VariableInstantiator variableInstantiator) {
		getRootPropertyGroup().addPropertyName(simplePropertyName, variableInstantiator);
	}

	public List<String> allPropertyPaths() {
		return getRootPropertyGroup().allPropertyPaths();
	}

	public VariableInstantiator getVariableInstantiator(String propertyName) {
		return getRootPropertyGroup().getVariableInstantiator(propertyName);
	}
	
	public List<String> allPropertyGroupPaths() {
		return getRootPropertyGroup().allPropertyGroupPaths();
	}

	@SuppressWarnings("rawtypes")
	private List<Class<? extends Command>> providedCmdClasses = 
			new ArrayList<Class<? extends Command>>();
	
	protected void registerModulePluginCmdClass(Class<? extends ModulePluginCommand<?, P>> providedCmdClass) {
		providedCmdClasses.add(providedCmdClass);
		commandGroupRegistry.registerCommandClass(providedCmdClass);
	}

	protected void registerModuleDocumentCmdClass(Class<? extends ModuleDocumentCommand<?>> providedCmdClass) {
		providedCmdClasses.add(providedCmdClass);
		commandGroupRegistry.registerCommandClass(providedCmdClass);
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

	public void loadResources(ConsoleCommandContext consoleCmdContext, File resourceDir, Module module) {
		// delete old resources.
		List<ModuleResource> oldResources = new LinkedList<ModuleResource>(module.getResources());
		for(ModuleResource moduleResource: oldResources) {
			GlueDataObject.delete(consoleCmdContext, ModuleResource.class, moduleResource.pkMap(), false);
		}
		if(!oldResources.isEmpty()) {
			consoleCmdContext.commit();
		}
		for(String resourceName: resourceNames) {
			File resourceFile = new File(resourceDir, resourceName);
			byte[] resourceContent = ConsoleCommandContext.loadBytesFromFile(resourceFile);
			ModuleResource moduleResource = GlueDataObject.create(consoleCmdContext, ModuleResource.class, 
						ModuleResource.pkMap(moduleName, resourceName), false);
			moduleResource.setContent(resourceContent);
			moduleResource.setModule(module);
		}		
		if(!resourceNames.isEmpty()) {
			consoleCmdContext.commit();
		}
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}
	
	public byte[] getResource(CommandContext cmdContext, String resourceName) {
		ModuleResource moduleResource = GlueDataObject.lookup(cmdContext, ModuleResource.class, ModuleResource.pkMap(moduleName, resourceName), true);
		if(moduleResource == null) {
			throw new ModuleException(ModuleException.Code.RESOURCE_NOT_LOADED, moduleName, resourceName);
		}
		return moduleResource.getContent();
	}
	
	protected PropertyGroup getRootPropertyGroup() {
		return rootPropertyGroup;
	}
	
	public boolean validProperty(String propertyPath) {
		List<String> propertyPathElems = Arrays.asList(propertyPath.split("/"));
		return getRootPropertyGroup().validProperty(propertyPathElems);
	}

	public boolean validPropertyGroup(String propertyPath) {
		List<String> propertyPathElems = Arrays.asList(propertyPath.split("/"));
		return getRootPropertyGroup().validPropertyGroup(propertyPathElems);
	}

	public void checkProperty(String propertyPath) {
		if(!validProperty(propertyPath)) {
			throw new ModuleException(ModuleException.Code.NO_SUCH_MODULE_PROPERTY, propertyPath);
		}
	}

	public void checkPropertyGroup(String propertyPath) {
		if(!validPropertyGroup(propertyPath)) {
			throw new ModuleException(ModuleException.Code.NO_SUCH_MODULE_PROPERTY_GROUP, propertyPath);
		}
	}

	public void init(CommandContext cmdContext) {}


	public void setCmdGroup(CommandGroup cmdGroup) {
		this.commandGroupRegistry.setCmdGroup(cmdGroup);
	}

	public CommandGroupRegistry getCommandGroupRegistry() {
		return commandGroupRegistry;
	}

	
}
