package uk.ac.gla.cvr.gluetools.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.classloader.GlueClassLoader;
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
import uk.ac.gla.cvr.gluetools.programs.mafft.MafftUtils;
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
	private Map<String, byte[]> classNameToBytes = new LinkedHashMap<String, byte[]>();
	private GlueClassLoader glueClassLoader = new GlueClassLoader(GluetoolsEngine.class.getClassLoader(), this);
	private ExecutorService mafftExecutorService;

	
	private GluetoolsEngine(String configFilePath) {
		gluecoreProperties = new Properties();
		try (InputStream propertiesStream = this.getClass().getResourceAsStream("/gluecore.properties")) {
			gluecoreProperties.load(propertiesStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		freemarkerConfiguration = new Configuration(Configuration.VERSION_2_3_24);
		freemarkerConfiguration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/freemarker");
		Document configDocument = null;
		if(configFilePath != null) {
			configDocument = GlueXmlUtils.documentFromBytes(ConsoleCommandContext.loadBytesFromFile(new File(configFilePath)));
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
						mysqlLine = "mysql -h 127.0.0.1";
					}
					String username = dbConfiguration.getUsername().orElse("<username>");
					String password = dbConfiguration.getPassword().orElse("<password>");
					String dbName = "<dbName>";
					String jdbcUrl = dbConfiguration.getJdbcUrl();
					if(jdbcUrl.contains("/")) {
						dbName = jdbcUrl.substring(jdbcUrl.lastIndexOf("/")+1);
						if(dbName.contains("?")) {
							dbName = dbName.substring(0, dbName.indexOf('?'));
						}
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
		if(mafftExecutorService != null) {
			mafftExecutorService.shutdown();
		}
	}

	public Properties getGluecoreProperties() {
		return gluecoreProperties;
	}

	public Configuration getFreemarkerConfiguration() {
		return freemarkerConfiguration;
	}

	public void addClass(String className, byte[] bytes) {
		synchronized(classNameToBytes) {
			classNameToBytes.put(className, bytes);
		}
	}

	public boolean containsClass(String className) {
		synchronized(classNameToBytes) {
			return classNameToBytes.containsKey(className);
		}
	}

	public byte[] getBytes(String className) {
		synchronized(classNameToBytes) {
			return classNameToBytes.get(className);
		}
	}

	public <X> X runWithGlueClassloader(Supplier<X> supplier) {
		ClassLoader prevContextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			// not sure what the effects are of not parenting to the current context classloader here
			// might need to revisit that.
			Thread.currentThread().setContextClassLoader(glueClassLoader);
			return supplier.get();
		} finally {
			Thread.currentThread().setContextClassLoader(prevContextClassLoader);
		}
	}
	
	// used to parellize mafft.
	public synchronized ExecutorService getMafftExecutorService() {
		if(mafftExecutorService == null) {
			int mafftCpus = Integer.parseInt(getPropertiesConfiguration().getPropertyValue(MafftUtils.MAFFT_NUMBER_CPUS, "1"));
			mafftExecutorService = Executors.newFixedThreadPool(mafftCpus);
		}
		return mafftExecutorService;
	}
	
	
}
