package uk.ac.gla.cvr.gluetools.core.datamodel.builder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.merge.AddColumnToDb;
import org.apache.cayenne.merge.DropColumnToDb;
import org.apache.cayenne.merge.DropTableToDb;
import org.apache.cayenne.merge.ExecutingMergerContext;
import org.apache.cayenne.merge.MergerContext;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.resource.ResourceLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.dataconnection.DatabaseConfiguration;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilderException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.meta.SchemaVersion;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.resource.GlueResourceLocator;
import uk.ac.gla.cvr.gluetools.core.resource.GlueResourceMap;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils.XmlNamespaceContext;

public class ModelBuilder {


	public static String META_DOMAIN_RESOURCE = "cayenne-gluemeta-domain.xml";
	public static String META_MAP_RESOURCE = "gluemeta-map.map.xml";

	public static String CORE_DOMAIN_RESOURCE = "cayenne-gluecore-domain.xml";
	public static String CORE_MAP_RESOURCE = "gluecore-map.map.xml";

	public static String PROJECT_DOMAIN_RESOURCE = "cayenne-glueproject-domain.xml";
	public static String PROJECT_MAP_RESOURCE = "glueproject-map.map.xml";

	private static String CAYENNE_NS = "http://cayenne.apache.org/schema/3.0/modelMap";
	

	public static ServerRuntime createMetaRuntime(DatabaseConfiguration dbConfiguration) {
		return new ServerRuntime(META_DOMAIN_RESOURCE, dbConfigModule(dbConfiguration));
	}
	
	public static ServerRuntime createRootRuntime(DatabaseConfiguration dbConfiguration) {
		return new ServerRuntime(CORE_DOMAIN_RESOURCE, dbConfigModule(dbConfiguration));
	}

	public static String getDbSchemaVersionString(ObjectContext metaObjectContext) {
		SchemaVersion schemaVersion = GlueDataObject.lookup(metaObjectContext, SchemaVersion.class, SchemaVersion.pkMap(1), true);
		if(schemaVersion != null) {
			return schemaVersion.getSchemaVersion();
		} else {
			return "0";
		}
	}

	public static void setDbSchemaVersionString(ObjectContext metaObjectContext, String versionString) {
		SchemaVersion schemaVersion = GlueDataObject.lookup(metaObjectContext, SchemaVersion.class, SchemaVersion.pkMap(1), true);
		if(schemaVersion == null) {
			schemaVersion = GlueDataObject.create(metaObjectContext, SchemaVersion.class, SchemaVersion.pkMap(1), false);
		}
		schemaVersion.setSchemaVersion(versionString);
		
	}


	private static Module dbConfigModule(DatabaseConfiguration dbConfiguration) {
		Module dbConfigModule = new Module() {
			  @Override
			  public void configure(Binder binder) {
			    MapBuilder<Object> map = binder.bindMap(Constants.PROPERTIES_MAP)
			       .put(Constants.JDBC_DRIVER_PROPERTY, dbConfiguration.getVendor().getJdbcDriverClass())
			       .put(Constants.JDBC_URL_PROPERTY, dbConfiguration.getJdbcUrl());
			    dbConfiguration.getUsername().ifPresent(u -> map.put(Constants.JDBC_USERNAME_PROPERTY, u));
			    dbConfiguration.getPassword().ifPresent(p -> map.put(Constants.JDBC_PASSWORD_PROPERTY, p));
			  }
		};
		return dbConfigModule;
	}
	
	
	public static ServerRuntime createProjectModel(DatabaseConfiguration dbConfiguration, Project project) {
		String projectName = project.getName();
		List<Field> fields = project.getFields();
		List<String> projectTableNames = new ArrayList<String>();
		Document cayenneDomainDocument;
		try(InputStream domainInputStream = ModelBuilder.class.getResourceAsStream("/"+PROJECT_DOMAIN_RESOURCE)) {
			cayenneDomainDocument = GlueXmlUtils.documentFromStream(domainInputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
		String projectDomainName = "cayenne-project-"+projectName+"-domain.xml";
		String projectMapName = projectMapName(projectName);
		Element domainElem = cayenneDomainDocument.getDocumentElement();
		GlueXmlUtils.findChildElements(domainElem, "map").get(0).setAttribute("name", projectMapName);
		Element nodeElem = GlueXmlUtils.findChildElements(domainElem, "node").get(0);
		GlueXmlUtils.findChildElements(nodeElem, "map-ref").get(0).setAttribute("name", projectMapName);
		
		Document cayenneMapDocument;
		try(InputStream domainInputStream = ModelBuilder.class.getResourceAsStream("/"+PROJECT_MAP_RESOURCE)) {
			cayenneMapDocument = GlueXmlUtils.documentFromStream(domainInputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
		XPath xPath = GlueXmlUtils.createXPathEngine();
		XmlNamespaceContext namespaceContext = new GlueXmlUtils.XmlNamespaceContext();
		namespaceContext.addNamespace("cay", CAYENNE_NS);
		xPath.setNamespaceContext(namespaceContext);
		{
			XPathExpression xPathExpression = 
					GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-entity[@name='SEQUENCE']");
			Element sequenceTableElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocument, xPathExpression);
			fields.forEach(f -> {
				Element dbAttributeElem = GlueXmlUtils.appendElementNS(sequenceTableElem, CAYENNE_NS, "db-attribute");
				dbAttributeElem.setAttribute("name", f.getName());
				dbAttributeElem.setAttribute("type", f.getFieldType().cayenneType());
				Optional.ofNullable(f.getMaxLength()).ifPresent(
						len -> { dbAttributeElem.setAttribute("length", Integer.toString(len)); });
			});
		}
		{
			XPathExpression xPathExpression = 
					GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:obj-entity[@name='Sequence']");
			Element sequenceObjElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocument, xPathExpression);
			fields.forEach(f -> {
				Element objAttributeElem = GlueXmlUtils.appendElementNS(sequenceObjElem, CAYENNE_NS, "obj-attribute");
				objAttributeElem.setAttribute("name", f.getName());
				objAttributeElem.setAttribute("db-attribute-path", f.getName());
				objAttributeElem.setAttribute("type", f.getFieldType().javaType());
			});
		}
		{
			XPathExpression xPathExpression = 
					GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-relationship");
			GlueXmlUtils.getXPathNodes(cayenneMapDocument, xPathExpression).forEach(n -> {
				Element dbRelationshipElem = (Element) n;
				specializeAttribute(projectName, dbRelationshipElem, "source");
				specializeAttribute(projectName, dbRelationshipElem, "target");
				
			});
		}
		{
			XPathExpression xPathExpression = 
					GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-entity");
			GlueXmlUtils.getXPathNodes(cayenneMapDocument, xPathExpression).forEach(n -> {
				Element dbEntityElem = (Element) n;
				specializeAttribute(projectName, dbEntityElem, "name");
				projectTableNames.add(dbEntityElem.getAttribute("name"));
			});
		}

		{
			XPathExpression xPathExpression = 
					GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:obj-entity");
			GlueXmlUtils.getXPathNodes(cayenneMapDocument, xPathExpression).forEach(n -> {
				Element objEntityElem = (Element) n;
				specializeAttribute(projectName, objEntityElem, "dbEntityName");
				
			});
		}

		
		GlueResourceMap.getInstance().put("/"+projectDomainName, GlueXmlUtils.prettyPrint(cayenneDomainDocument));
		GlueResourceMap.getInstance().put("/"+projectMapName+".map.xml", GlueXmlUtils.prettyPrint(cayenneMapDocument));

		// XmlUtils.prettyPrint(cayenneDomainDocument, System.out);
		// XmlUtils.prettyPrint(cayenneMapDocument, System.out);

		ServerRuntime projectRuntime = new ServerRuntime(
					    projectDomainName, 
					    dbConfigModule(dbConfiguration),
					     binder -> binder.bind(ResourceLocator.class)
					                     .to(GlueResourceLocator.class));
		// ensure it is created by getting the obj context.
		projectRuntime.getContext();
		Set<String> tableNamesInDB; 
		try {
			tableNamesInDB = getNameTablesInDB(projectRuntime.getDataDomain().getDataNode("glueproject-node"));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		for(String projectTableName: projectTableNames) {
			if(tableNamesInDB.contains(projectTableName)) {
				// System.out.println("Found table "+projectTableName);
			} else {
				// should not happen in practice.
				// put this in to help debug Cayenne problems.
				throw new ModelBuilderException(Code.PROJECT_TABLE_MISSING, projectName, projectTableName);
			}
		}
		return projectRuntime;
	}

	// Not sure why the project has to have a different map name.
	public static String projectMapName(String projectName) {
		String projectMapName = "project-"+projectName+"-map";
		return projectMapName;
	}
	
	
	private static void specializeAttribute(String projectName, Element elem, String attrName) {
		String tableName = elem.getAttribute(attrName);
		elem.setAttribute(attrName, specializeTableName(tableName, projectName));
	}

	public static String specializeTableName(String tableName,
			String projectName) {
		return projectName+"_"+tableName;
	}

	
	private static Set<String> getNameTablesInDB(DataNode dataNode)
            throws SQLException {
        String tableLabel = dataNode.getAdapter().tableTypeForTable();
        Connection con = null;
        Set<String> nameTables = new LinkedHashSet<String>();
        con = dataNode.getDataSource().getConnection();

        try {
            ResultSet rs = con.getMetaData().getTables(null, null, "%", new String[] {
                tableLabel
            });

            try {

                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    nameTables.add(name);
                }
            }
            finally {
                rs.close();
            }

        }
        finally {

            con.close();
        }
        return nameTables;
    }


	
	public static void deleteProjectModel(DatabaseConfiguration dbConfiguration, Project project) {
		ServerRuntime projectRuntime = null;
		try {
			projectRuntime = createProjectModel(dbConfiguration, project);
			MergerContext mergerContext = getMergerContext(project, projectRuntime);

			List<MergerToken> tokens = new ArrayList<MergerToken>();
			for(DbEntity dbEntity: mergerContext.getDataMap().getDbEntities()) {
				tokens.add(new DropTableToDb(dbEntity));
			}
			for (MergerToken tok : tokens) {
				tok.execute(mergerContext);
			} 
		} finally {
			if(projectRuntime != null) {
				projectRuntime.shutdown();
			}
		}
	}
	
	public static void addSequenceColumnToModel(DatabaseConfiguration dbConfiguration, Project project, Field field) {
		ServerRuntime projectRuntime = null;
		try {
			projectRuntime = createProjectModel(dbConfiguration, project);
			MergerContext mergerContext = getMergerContext(project, projectRuntime);
			AddColumnToDb addToken = getAddColumnToken(project, field, mergerContext);
			addToken.execute(mergerContext);
		} finally {
			if(projectRuntime != null) {
				projectRuntime.shutdown();
			}
		}
	}

	private static AddColumnToDb getAddColumnToken(Project project, Field field, MergerContext mergerContext) {
		DbEntity sequenceTable = 
				mergerContext.getDataMap().getDbEntity(specializeTableName("SEQUENCE", project.getName()));
		DbAttribute column = new DbAttribute(field.getName());
		column.setType(TypesMapping.getSqlTypeByName(field.getFieldType().cayenneType()));
		Integer maxLength = field.getMaxLength();
		if(maxLength != null) {
			column.setMaxLength(maxLength);
		}
		column.setEntity(sequenceTable);
		return new AddColumnToDb(sequenceTable, column);
	}

	private static MergerContext getMergerContext(Project project,
			ServerRuntime projectRuntime) {
		DataDomain domain = projectRuntime.getDataDomain();
		DataNode node = domain.getDataNode("glueproject-node");
		DataMap map = domain.getDataMap(projectMapName(project.getName()));
		MergerContext mergerContext = new ExecutingMergerContext(map, node);
		return mergerContext;
	}

	public static void deleteSequenceColumnFromModel(DatabaseConfiguration dbConfiguration, Project project, Field field) {
		ServerRuntime projectRuntime = null;
		try {
			projectRuntime = createProjectModel(dbConfiguration, project);
			MergerContext mergerContext = getMergerContext(project, projectRuntime);
			AddColumnToDb addToken = getAddColumnToken(project, field, mergerContext);
			DropColumnToDb dropToken = new DropColumnToDb(addToken.getEntity(), addToken.getColumn());
			dropToken.execute(mergerContext);
		} finally {
			if(projectRuntime != null) {
				projectRuntime.shutdown();
			}
		}
	}

}
