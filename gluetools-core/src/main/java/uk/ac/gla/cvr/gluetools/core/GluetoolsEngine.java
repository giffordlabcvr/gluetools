package uk.ac.gla.cvr.gluetools.core;

import java.io.File;
import java.util.logging.Logger;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException;
import uk.ac.gla.cvr.gluetools.core.dataconnection.DatabaseConfiguration;
import uk.ac.gla.cvr.gluetools.core.dataconnection.DatabaseConfiguration.Vendor;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.GlueSchemaUpdateStrategy;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilderException;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilderException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.meta.SchemaVersion;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import freemarker.template.Configuration;

public class GluetoolsEngine implements Plugin {

	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.core");

	private static GluetoolsEngine instance;
	
	private String dbSchemaVersion;
	
	public static synchronized GluetoolsEngine initInstance(String configFilePath, boolean migrateSchema) {
		if(instance != null) {
			throw new GluetoolsEngineException(GluetoolsEngineException.Code.ENGINE_ALREADY_INITIALIZED);
		}
		instance = new GluetoolsEngine(configFilePath);
		instance.init(migrateSchema);
		return instance;
	}

	public static synchronized GluetoolsEngine getInstance() {
		if(instance == null) {
			throw new GluetoolsEngineException(GluetoolsEngineException.Code.ENGINE_NOT_INITIALIZED);
		}
		return instance;
	}

	
	private Configuration freemarkerConfiguration;
	private DatabaseConfiguration dbConfiguration = new DatabaseConfiguration();
	private ServerRuntime rootServerRuntime;
	
	private GluetoolsEngine(String configFilePath) {
		freemarkerConfiguration = new Configuration();
		Document configDocument = null;
		if(configFilePath != null) {
			try {
				configDocument = GlueXmlUtils.documentFromBytes(ConsoleCommandContext.loadBytesFromFile(new File(configFilePath)));
			} catch(SAXException saxe) {
				throw new ConsoleException(ConsoleException.Code.GLUE_CONFIG_XML_FORMAT_ERROR, saxe.getLocalizedMessage());
			}
		} else {
			configDocument = GlueXmlUtils.documentWithElement("gluetools").getOwnerDocument();
		}
		configure(createPluginConfigContext(), configDocument.getDocumentElement());
	}
	
	public PluginConfigContext createPluginConfigContext() {
		return new PluginConfigContext(freemarkerConfiguration);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		Element dbConfigElem = PluginUtils.findConfigElement(configElem, "database");
		if(dbConfigElem != null) {
			dbConfiguration.configure(pluginConfigContext, dbConfigElem);
		}
		if(dbConfiguration.getVendor() == Vendor.ApacheDerby &&
				dbConfiguration.getJdbcUrl().contains(":memory:")) {
			logger.warning("The GLUE database is in-memory Apache Derby. "+
				"Changes will not be persisted beyond the lifetime of this GLUE instance.");
		}

	}

	public DatabaseConfiguration getDbConfiguration() {
		return dbConfiguration;
	}
	
	private void init(boolean migrateSchema) {
		ServerRuntime metaRuntime = null;
		try {
			metaRuntime = ModelBuilder.createMetaRuntime(dbConfiguration);
			ObjectContext metaObjectContext = GlueDataObject.createObjectContext(metaRuntime);
			dbSchemaVersion = ModelBuilder.getDbSchemaVersionString(metaObjectContext);
			String currentSchemaVersion = SchemaVersion.currentVersionString;
			if(SchemaVersion.isLaterThanCurrent(dbSchemaVersion)) {
				throw new ModelBuilderException(Code.SCHEMA_VERSION_LATER_THAN_CURRENT, dbSchemaVersion, currentSchemaVersion);
			}
			if(!dbSchemaVersion.equals("0") && !dbSchemaVersion.equals(currentSchemaVersion)) {
				if(GlueSchemaUpdateStrategy.canMigrateFromSchemaVersion(dbSchemaVersion)) {
					if(!migrateSchema) {
						throw new ModelBuilderException(Code.MIGRATE_SCHEMA_OPTION_REMINDER, dbSchemaVersion, currentSchemaVersion);
					}
				} else {
					throw new ModelBuilderException(Code.SCHEMA_MIGRATION_NOT_IMPLEMENTED, dbSchemaVersion, currentSchemaVersion);
				}
			}
			rootServerRuntime = ModelBuilder.createRootRuntime(dbConfiguration);
			rootServerRuntime.getContext(); // just to check that it works.
			ModelBuilder.setDbSchemaVersionString(metaObjectContext, currentSchemaVersion);
			metaObjectContext.commitChanges();
		} finally {
			if(metaRuntime != null) { metaRuntime.shutdown(); }
		}

	}

	public ServerRuntime getRootServerRuntime() {
		return rootServerRuntime;
	}

	public String getDbSchemaVersion() {
		return dbSchemaVersion;
	}

	public synchronized static void shutdown() {
		if(instance != null) {
			instance.dispose();
			instance = null;
		}
	}
	
	private void dispose() {
		rootServerRuntime.shutdown();
	}
	
	
	
}
