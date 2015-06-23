package uk.ac.gla.cvr.gluetools.core.datamodel.module;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Module;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException.Code;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

@GlueDataClass(defaultListColumns = {_Module.NAME_PROPERTY})
public class Module extends _Module {


	private Document configDoc = null;
	private ModulePlugin modulePlugin = null;
	
	public static Map<String, String> pkMap(String projectName, String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(PROJECT_PK_COLUMN, projectName);
		idMap.put(NAME_PK_COLUMN, name);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PK_COLUMN));
	}

	public ModulePlugin buildModulePlugin(PluginConfigContext pluginConfigContext) {
		Element rootElem = getConfigDoc().getDocumentElement();
		ModulePluginFactory importerPluginFactory = PluginFactory.get(ModulePluginFactory.creator);
		return importerPluginFactory.createFromElement(pluginConfigContext, rootElem);
	}

	public Document getConfigDoc() {
		if(configDoc == null) {
			configDoc = buildConfigDoc();
		}
		return configDoc;
	}
	
	public ModulePlugin getModulePlugin(PluginConfigContext pluginConfigContext) {
		if(modulePlugin == null) {
			modulePlugin = buildModulePlugin(pluginConfigContext);
		}
		return modulePlugin;
	}
	
	private Document buildConfigDoc() {
		byte[] config = getConfig();
		try {
			return XmlUtils.documentFromStream(new ByteArrayInputStream(config));
		} catch (SAXException e) {
			throw new PluginFactoryException(e, Code.PLUGIN_CONFIG_FORMAT_ERROR, e.getMessage());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
