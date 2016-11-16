package uk.ac.gla.cvr.gluetools.core.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleLoadConfigurationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleSaveConfigurationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleShowConfigurationCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleValidateCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleCreatePropertyGroupCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleDeletePropertyGroupCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleSetPropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleShowPropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.property.ModuleUnsetPropertyCommand;
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
	
	private String moduleName;
	
	public ModulePlugin() {
		super();
		addModuleDocumentCmdClass(ModuleValidateCommand.class);
		addModuleDocumentCmdClass(ModuleShowConfigurationCommand.class);
		addModuleDocumentCmdClass(ModuleSetPropertyCommand.class);
		addModuleDocumentCmdClass(ModuleUnsetPropertyCommand.class);
		addModuleDocumentCmdClass(ModuleShowPropertyCommand.class);

		addModuleDocumentCmdClass(ModuleCreatePropertyGroupCommand.class);
		addModuleDocumentCmdClass(ModuleDeletePropertyGroupCommand.class);

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

	// call this during configure to register a resource name
	// the resource will then get loaded during loadResources()
	protected void registerResourceName(String resourceName) {
		resourceNames.add(resourceName);
	}
	
	protected void addSimplePropertyName(String simplePropertyName) {
		getRootPropertyGroup().addPropertyName(simplePropertyName);
	}
	
	public List<String> allPropertyPaths() {
		return getRootPropertyGroup().allPropertyPaths();
	}

	public List<String> allPropertyGroupPaths() {
		return getRootPropertyGroup().allPropertyGroupPaths();
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

	public final void loadResources(ConsoleCommandContext consoleCmdContext, File resourceDir, Module module) {
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
	
	public class PropertyGroup {
		private List<String> propertyNames = new ArrayList<String>();
		private Map<String, PropertyGroup> children = new LinkedHashMap<String, PropertyGroup>();

		public PropertyGroup addPropertyName(String simplePropertyName) {
			this.propertyNames.add(simplePropertyName);
			return this;
		}
		
		public List<String> allPropertyPaths() {
			List<String> paths = new ArrayList<String>();
			paths.addAll(propertyNames);
			children.forEach((groupName, group) -> {
				paths.addAll(group.allPropertyPaths().stream().map(s -> groupName+"/"+s).collect(Collectors.toList()));
			});
			return paths;
		}

		public List<String> allPropertyGroupPaths() {
			List<String> paths = new ArrayList<String>();
			children.forEach((groupName, group) -> {
				paths.add(groupName);
				paths.addAll(group.allPropertyGroupPaths().stream().map(s -> groupName+"/"+s).collect(Collectors.toList()));
			});
			return paths;
		}

		public List<String> getPropertyNames() {
			return propertyNames;
		}

		public PropertyGroup addChild(String groupName) {
			PropertyGroup propertyGroup = new PropertyGroup();
			children.put(groupName, propertyGroup);
			return propertyGroup;
		}

		public PropertyGroup getChild(String groupName) {
			return children.get(groupName);
		}

		public boolean validProperty(List<String> propertyPathElems) {
			if(propertyPathElems.size() == 0) {
				return false;
			}
			if(propertyPathElems.size() == 1) {
				return propertyNames.contains(propertyPathElems.get(0));
			}
			PropertyGroup child = getChild(propertyPathElems.get(0));
			if(child == null) {
				return false;
			}
			return child.validProperty(propertyPathElems.subList(1, propertyPathElems.size()));
		}

		public boolean validPropertyGroup(List<String> propertyPathElems) {
			if(propertyPathElems.size() == 0) {
				return false;
			}
			if(propertyPathElems.size() == 1) {
				return children.keySet().contains(propertyPathElems.get(0));
			}
			PropertyGroup child = getChild(propertyPathElems.get(0));
			if(child == null) {
				return false;
			}
			return child.validPropertyGroup(propertyPathElems.subList(1, propertyPathElems.size()));
		}

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


}
