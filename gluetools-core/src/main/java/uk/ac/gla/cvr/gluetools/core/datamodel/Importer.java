package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.importing.ImporterPlugin;
import uk.ac.gla.cvr.gluetools.core.collation.importing.ImporterPluginFactory;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Importer;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException.Code;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

@GlueDataClass(listColumnHeaders = {_Importer.NAME_PROPERTY})
public class Importer extends _Importer {

	private Document configDoc = null;
	private ImporterPlugin importerPlugin = null;
	
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

	public ImporterPlugin buildPlugin(PluginConfigContext pluginConfigContext) {
		Element rootElem = getConfigDoc().getDocumentElement();
		ImporterPluginFactory importerPluginFactory = PluginFactory.get(ImporterPluginFactory.creator);
		return importerPluginFactory.createFromElement(pluginConfigContext, rootElem);
	}

	public Document getConfigDoc() {
		if(configDoc == null) {
			configDoc = buildConfigDoc();
		}
		return configDoc;
	}
	
	public ImporterPlugin getImporterPlugin(PluginConfigContext pluginConfigContext) {
		if(importerPlugin == null) {
			importerPlugin = buildPlugin(pluginConfigContext);
		}
		return importerPlugin;
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