package uk.ac.gla.cvr.gluetools.core;

import java.net.URL;
import java.util.logging.Logger;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Element;

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
import uk.ac.gla.cvr.gluetools.core.resource.GlueURLStreamHandlerFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;
import freemarker.template.Configuration;

public class GluetoolsEngine implements Plugin {

	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.core");

	private static Multiton instances = new Multiton();
	
	private static Multiton.Creator<GluetoolsEngine> creator = new
			Multiton.SuppliedCreator<>(GluetoolsEngine.class, GluetoolsEngine::new);
	
	private String dbSchemaVersion;
	
	public static GluetoolsEngine getInstance() {
		return instances.get(creator);
	}
	
	private Configuration freemarkerConfiguration;
	private DatabaseConfiguration dbConfiguration = new DatabaseConfiguration();
	private ServerRuntime rootServerRuntime;
	
	private GluetoolsEngine() {
		freemarkerConfiguration = new Configuration();
		URL.setURLStreamHandlerFactory(new GlueURLStreamHandlerFactory());
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
	
	public void init(boolean migrateSchema) {
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


}
