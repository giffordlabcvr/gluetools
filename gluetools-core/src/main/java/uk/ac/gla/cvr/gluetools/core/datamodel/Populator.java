package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.populating.PopulatorPlugin;
import uk.ac.gla.cvr.gluetools.core.collation.populating.PopulatorPluginFactory;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Populator;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException.Code;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

@GlueDataClass(listColumnHeaders = {_Populator.NAME_PROPERTY})
public class Populator extends _Populator {
	private Document configDoc = null;
	private PopulatorPlugin populatorPlugin = null;
	
	@Override
	public String[] populateListRow() {
		return new String[]{getName()};
	}

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

	public PopulatorPlugin buildPlugin(PluginConfigContext pluginConfigContext) {
		Element rootElem = getConfigDoc().getDocumentElement();
		PopulatorPluginFactory populatorPluginFactory = PluginFactory.get(PopulatorPluginFactory.creator);
		return populatorPluginFactory.createFromElement(pluginConfigContext, rootElem);
	}

	public Document getConfigDoc() {
		if(configDoc == null) {
			configDoc = buildConfigDoc();
		}
		return configDoc;
	}
	
	public PopulatorPlugin getPopulatorPlugin(PluginConfigContext pluginConfigContext) {
		if(populatorPlugin == null) {
			populatorPlugin = buildPlugin(pluginConfigContext);
		}
		return populatorPlugin;
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
