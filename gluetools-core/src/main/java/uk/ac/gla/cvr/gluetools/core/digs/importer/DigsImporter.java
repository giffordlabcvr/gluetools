package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.config.DatabaseConfiguration;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.digs.importer.DigsImporterException.Code;
import uk.ac.gla.cvr.gluetools.core.digs.importer.model.DigsObject;
import uk.ac.gla.cvr.gluetools.core.digs.importer.model.Extracted;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.utils.CayenneUtils;

@PluginClass(elemName="digsImporter")
public class DigsImporter extends ModulePlugin<DigsImporter> {

	
	public static String 
		DIGS_DB_JDBC_URL_PROPERTY = "gluetools.core.digs.db.jdbc.url"; 
	public static String 
		DIGS_DB_USERNAME_PROPERTY = "gluetools.core.digs.db.username"; 
	public static String 
		DIGS_DB_PASSWORD_PROPERTY = "gluetools.core.digs.db.password"; 

	public static String 
		DIGS_DOMAIN_RESOURCE = "cayenne-digs-domain.xml";
	public static String 
		DIGS_MAP_RESOURCE = "digs-map.map.xml";
	
	
	public DigsImporter() {
		super();
		addProvidedCmdClass(ListExtractedCommand.class);
		addProvidedCmdClass(ImportExtractedCommand.class);
		addProvidedCmdClass(ListDigsDbsCommand.class);
	}

	private <C extends DigsObject> List<C> query(ObjectContext digsObjectContext, Class<C> objClass, SelectQuery query) {
		List<?> queryResult = null;
		try {
			queryResult = digsObjectContext.performQuery(query);
		} catch(CayenneRuntimeException cre) {
			Throwable cause = cre.getCause();
			Expression qualifier = query.getQualifier();
			if(qualifier != null && cause != null && cause instanceof ExpressionException) {
				throw new DigsImporterException(Code.EXPRESSION_ERROR, qualifier.toString(), cause.getMessage());
			} else {
				throw cre;
			}
		}
		return queryResult.stream().map(obj -> { 
			C dataObject = objClass.cast(obj);
			return dataObject;
		}).collect(Collectors.toList());
	}
	

	private List<Extracted> listExtracted(CommandContext cmdContext, String digsDbName, Optional<Expression> whereClause) {
		ServerRuntime digsServerRuntime = null;
		try {
			digsServerRuntime = createDigsServerRuntime(cmdContext, digsDbName);
			ObjectContext digsObjectContext = digsServerRuntime.getContext();
			SelectQuery selectQuery = null;
			if(whereClause.isPresent()) {
				selectQuery = new SelectQuery(Extracted.class, whereClause.get());
			} else {
				selectQuery = new SelectQuery(Extracted.class);
			}
			return query(digsObjectContext, Extracted.class, selectQuery);
		} finally {
			if(digsServerRuntime != null) {
				try { digsServerRuntime.shutdown(); } 
				catch(Exception e) {
					GlueLogger.getGlueLogger().warning("Exception thrown while shutting down DIGS MySQL connection: "+e.getLocalizedMessage());
				}
			}
		}
	}
	
	public ImportExtractedResult importHits(CommandContext cmdContext, String digsDbName) {
		List<Extracted> extracted = listExtracted(cmdContext, digsDbName, Optional.ofNullable(null));
		List<Map<String, Object>> rowData = extracted.stream()
				.map(extr -> {
					Map<String, Object> map = new LinkedHashMap<String, Object>();
					map.put(ImportExtractedResult.BLAST_ID, extr.getBlastId());
					map.put(ImportExtractedResult.SEQUENCE, extr.getSequence());
					return map;
				})
				.collect(Collectors.toList());
		return new ImportExtractedResult(rowData);
	}

	public static List<String> listDigsDatabases(CommandContext cmdContext) {
		
		String digsDbBaseUrl = getDigsBaseUrl(cmdContext);
		String digsDbUsername = getDigsDbUsername(cmdContext);
		String digsDbPassword = getDigsDbPassword(cmdContext);
		try {
			// ensure MySQL driver loaded
			Class.forName(DatabaseConfiguration.Vendor.MySQL.getJdbcDriverClass()).newInstance();
		} catch(Exception e) {
			throw new DigsImporterException(e, Code.DIGS_DB_ERROR, e.getLocalizedMessage());
		}
		Connection connection = null;
		List<String> digsDbNames = new ArrayList<String>();
		try {
			connection = DriverManager.getConnection(digsDbBaseUrl, digsDbUsername, digsDbPassword);
			DatabaseMetaData dbMetadata = connection.getMetaData();
			ResultSet catalogs = dbMetadata.getCatalogs();
			while (catalogs.next()) {
				digsDbNames.add(catalogs.getString(1));
			}
		} catch(SQLException sqle) {
			throw new DigsImporterException(sqle, Code.DIGS_DB_ERROR, sqle.getLocalizedMessage());
		} finally {
			if(connection != null) {
				try { connection.close(); }
				catch(SQLException sqle) {
					GlueLogger.getGlueLogger().warning("Exception closing direct JDBC connection to DIGS MySQL DB: "+sqle.getLocalizedMessage());
				}
			}
		}
		digsDbNames.removeAll(Arrays.asList("information_schema", "mysql", "performance_schema"));
		return digsDbNames;
	}
	
	private ServerRuntime createDigsServerRuntime(CommandContext cmdContext, String digsDbName) {
		String baseUrl = getDigsBaseUrl(cmdContext);
		String digsDbJdbcUrl = baseUrl+"/"+digsDbName;
		String digsDbUsername = getDigsDbUsername(cmdContext);
		String digsDbPassword = getDigsDbPassword(cmdContext);
		return new ServerRuntime(DIGS_DOMAIN_RESOURCE, 
				CayenneUtils.createCayenneDbConfigModule("20000", DatabaseConfiguration.Vendor.MySQL.getJdbcDriverClass(), 
				digsDbJdbcUrl, Optional.of(digsDbUsername), Optional.of(digsDbPassword)));
	}

	public static String getDigsDbPassword(CommandContext cmdContext) {
		PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String digsDbPassword = propertiesConfiguration.getPropertyValue(DIGS_DB_PASSWORD_PROPERTY);
		if(digsDbPassword == null) {
			throw new DigsImporterException(Code.DIGS_DB_JDBC_PASSWORD_NOT_DEFINED);
		}
		return digsDbPassword;
	}

	public static String getDigsDbUsername(CommandContext cmdContext) {
		PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String digsDbUsername = propertiesConfiguration.getPropertyValue(DIGS_DB_USERNAME_PROPERTY);
		if(digsDbUsername == null) {
			throw new DigsImporterException(Code.DIGS_DB_JDBC_USER_NOT_DEFINED);
		}
		return digsDbUsername;
	}

	public static String getDigsBaseUrl(CommandContext cmdContext) {
		PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String baseUrl = propertiesConfiguration.getPropertyValue(DIGS_DB_JDBC_URL_PROPERTY);
		if(baseUrl == null) {
			throw new DigsImporterException(Code.DIGS_DB_JDBC_URL_NOT_DEFINED);
		}
		return baseUrl;
	}

	public ListExtractedResult listExtracted(CommandContext cmdContext, String digsDbName, Optional<Expression> whereClause, List<String> fieldNames) {
		List<Extracted> extracteds = listExtracted(cmdContext, digsDbName, whereClause);
		return new ListExtractedResult(extracteds, fieldNames);
	}
	
	
}
