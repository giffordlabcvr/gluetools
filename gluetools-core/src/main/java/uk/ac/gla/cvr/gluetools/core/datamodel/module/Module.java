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
package uk.ac.gla.cvr.gluetools.core.datamodel.module;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroupRegistry;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException.Code;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@GlueDataClass(defaultListedProperties = {_Module.NAME_PROPERTY})
public class Module extends _Module {


	private byte[] configBytes = null;
	private Document configDoc = null;
	private ModulePlugin<?> modulePlugin = null;
	private boolean valid = false;
	
	public static Map<String, String> pkMap(String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}

	private ModulePlugin<?> buildModulePlugin() {
		Element rootElem = getConfigDoc().getDocumentElement();
		ModulePluginFactory importerPluginFactory = PluginFactory.get(ModulePluginFactory.creator);
		return importerPluginFactory.instantiateFromElement(rootElem);
	}

	public Document getConfigDoc() {
		if(configDoc == null) {
			configDoc = buildConfigDoc();
		}
		return configDoc;
	}
	
	public ModulePlugin<?> getModulePlugin(CommandContext cmdContext) {
		return getModulePlugin(cmdContext, true);
	}

	public ModulePlugin<?> getModulePlugin(CommandContext cmdContext, boolean requireValid) {
		if(modulePlugin == null) {
			modulePlugin = buildModulePlugin();
			modulePlugin.setModuleName(getName());
		}
		if(requireValid == true && !valid) {
			PluginFactory.configurePlugin(cmdContext.getGluetoolsEngine().createPluginConfigContext(), configDoc.getDocumentElement(), modulePlugin);
			modulePlugin.init(cmdContext);
			valid = true;
		}
		return modulePlugin;
	}

	
	private Document buildConfigDoc() {
		byte[] config = getConfig();
		try {
			return GlueXmlUtils.documentFromStream(new ByteArrayInputStream(config));
		} catch (SAXException e) {
			throw new PluginFactoryException(Code.PLUGIN_CONFIG_FORMAT_ERROR, e.getMessage());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public List<Class<? extends Command>> getProvidedCommandClasses(CommandContext cmdContext) {
		return getModulePlugin(cmdContext, false).getProvidedCommandClasses();
	}
	
	public CommandGroupRegistry getCommandGroupRegistry(CommandContext cmdContext) {
		return getModulePlugin(cmdContext, false).getCommandGroupRegistry();
	}

	public String getType() {
		return getConfigDoc().getDocumentElement().getNodeName();
	}
	
	@Override
	public void writePropertyDirectly(String propName, Object val) {
		if(propName.equals(CONFIG_PROPERTY)) {
			if((configBytes == null && val != null) ||
				(configBytes != null && val == null) ||
				(configBytes != null && val != null && !Arrays.equals(configBytes, (byte[]) val))) {
				// updating the config bytes invalidates the cached document / plugin / valid flag.
				configDoc = null;
				modulePlugin = null;
				valid = false;
				configBytes = (byte[]) val;
			}
		}
		super.writePropertyDirectly(propName, val);
	}
	
	public void validate(CommandContext cmdContext) {
		ModulePlugin<?> modulePlugin = getModulePlugin(cmdContext);
		modulePlugin.validate(cmdContext);
	}
	
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(getName());
	}

	@SuppressWarnings("unchecked")
	public static <M> M resolveModulePlugin(CommandContext cmdContext, Class<M> requiredClass, String moduleName) {
		if(moduleName == null) {
			throw new ModuleException(ModuleException.Code.NO_MODULE_DEFINED);
		}
		Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(moduleName));
		ModulePlugin<?> modulePlugin = module.getModulePlugin(cmdContext);
		Class<?> actualClass = modulePlugin.getClass();
		if(!(requiredClass.isAssignableFrom(actualClass))) {
			throw new ModuleException(ModuleException.Code.MODULE_PLUGIN_IS_NOT_OF_CORRECT_CLASS, moduleName, 
					requiredClass.getSimpleName(), actualClass.getSimpleName());
		}
		return (M) modulePlugin;

	}

	public void loadConfig(ConsoleCommandContext consoleCmdContext, String fileName, boolean loadResources) {
		File file = consoleCmdContext.fileStringToFile(fileName);
		byte[] config = ConsoleCommandContext.loadBytesFromFile(file);
		if(loadResources) {
			modulePlugin = null;
			valid = false;
		}
		setConfig(config);
		if(loadResources) {
			ModulePlugin<?> modulePlugin = getModulePlugin(consoleCmdContext, true);
			File resourceDir = file.getParentFile();
			modulePlugin.loadResources(consoleCmdContext, resourceDir, this);
		}
	}

}
