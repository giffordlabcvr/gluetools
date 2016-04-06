package uk.ac.gla.cvr.gluetools.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.config.DatabaseConfiguration;
import uk.ac.gla.cvr.gluetools.core.config.DatabaseConfiguration.Vendor;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.GlueSchemaUpdateStrategy;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilderException;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilderException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.meta.SchemaVersion;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import freemarker.template.Configuration;

public class GluetoolsEngine implements Plugin {

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
	private PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
	private ServerRuntime rootServerRuntime;
	private Properties gluecoreProperties;
	
	private GluetoolsEngine(String configFilePath) {
		gluecoreProperties = new Properties();
		try (InputStream propertiesStream = this.getClass().getResourceAsStream("/gluecore.properties")) {
			gluecoreProperties.load(propertiesStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		freemarkerConfiguration = new Configuration();
		Document configDocument = null;
		if(configFilePath != null) {
			try {
				configDocument = GlueXmlUtils.documentFromBytes(ConsoleCommandContext.loadBytesFromFile(new File(configFilePath)));
			} catch(SAXException saxe) {
				throw new GluetoolsEngineException(GluetoolsEngineException.Code.CONFIG_INVALID_XML, configFilePath, saxe.getLocalizedMessage());
			}
		} else {
			configDocument = GlueXmlUtils.documentWithElement("gluetools").getOwnerDocument();
		}
		try {
			configure(createPluginConfigContext(), configDocument.getDocumentElement());
		} catch(GlueException glueEx) {
			throw new GluetoolsEngineException(glueEx, GluetoolsEngineException.Code.CONFIG_ERROR, configFilePath, glueEx.getLocalizedMessage());
		}
	}
	
	public PluginConfigContext createPluginConfigContext() {
		return new PluginConfigContext(freemarkerConfiguration);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		Element dbConfigElem = PluginUtils.findConfigElement(configElem, "database");
		if(dbConfigElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, dbConfigElem, dbConfiguration);
		}
		Element propertiesConfigElem = PluginUtils.findConfigElement(configElem, "properties");
		if(propertiesConfigElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, propertiesConfigElem, propertiesConfiguration);
		}
	}

	public void dbWarning() {
		if(dbConfiguration.getVendor() == Vendor.ApacheDerby &&
				dbConfiguration.getJdbcUrl().contains(":memory:")) {
			GlueLogger.getGlueLogger().warning("The GLUE database is in-memory Apache Derby. "+
				"Changes will not be persisted beyond the lifetime of this GLUE instance.");
		}
	}

	public DatabaseConfiguration getDbConfiguration() {
		return dbConfiguration;
	}
	
	public PropertiesConfiguration getPropertiesConfiguration() {
		return propertiesConfiguration;
	}
	
	private void init(boolean migrateSchema) {
		ServerRuntime metaRuntime = null;
		try {
			metaRuntime = ModelBuilder.createMetaRuntime(dbConfiguration, propertiesConfiguration);
			ObjectContext metaObjectContext = metaRuntime.getContext();
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
					String osName = System.getProperty("os.name");
					String mysqlLine = "/path/to/mysql";
					if(osName.equals("Linux")) {
						mysqlLine = "mysql";
					} else if(osName.equals("Mac OS X")) {
						mysqlLine = "/usr/local/mysql/bin/mysql";
					} if(osName.toLowerCase().startsWith("windows")) {
						mysqlLine = "mysql";
					}
					String username = dbConfiguration.getUsername().orElse("<username>");
					String password = dbConfiguration.getPassword().orElse("<password>");
					String dbName = "<dbName>";
					String jdbcUrl = dbConfiguration.getJdbcUrl();
					if(jdbcUrl.contains("/")) {
						dbName = jdbcUrl.substring(jdbcUrl.lastIndexOf("/")+1);
					}
					throw new ModelBuilderException(Code.SCHEMA_MIGRATION_NOT_IMPLEMENTED, 
							dbSchemaVersion, currentSchemaVersion, mysqlLine, username, password, dbName);
				}
			}
			rootServerRuntime = ModelBuilder.createRootRuntime(dbConfiguration, propertiesConfiguration);
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

	public Properties getGluecoreProperties() {
		return gluecoreProperties;
	}

	
	
	
}
