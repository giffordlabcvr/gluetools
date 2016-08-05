package uk.ac.gla.cvr.gluetools.core.datamodel.builder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;

import org.apache.cayenne.DeleteDenyException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.merge.AddColumnToDb;
import org.apache.cayenne.merge.DropColumnToDb;
import org.apache.cayenne.merge.DropTableToDb;
import org.apache.cayenne.merge.ExecutingMergerContext;
import org.apache.cayenne.merge.MergerContext;
import org.apache.cayenne.merge.MergerToken;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.resource.ResourceLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.config.DatabaseConfiguration;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilderException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.meta.SchemaVersion;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.resource.GlueResourceLocator;
import uk.ac.gla.cvr.gluetools.core.resource.GlueResourceMap;
import uk.ac.gla.cvr.gluetools.utils.CayenneUtils;
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
	
	// tables within a project where fields can be added / deleted.
	public enum ConfigurableTable { 
		sequence(Sequence.class),
		variation(Variation.class),
		feature(Feature.class),
		alignment(Alignment.class),
		reference(ReferenceSequence.class),
		alignment_member(AlignmentMember.class),
		var_almt_note(VarAlmtNote.class);
		
		private Class<? extends GlueDataObject> dataObjectClass;

		private ConfigurableTable(Class <? extends GlueDataObject> dataObjectClass) {
			this.dataObjectClass = dataObjectClass;
		}

		public Class<? extends GlueDataObject> getDataObjectClass() {
			return dataObjectClass;
		}
		
	};
	
	public static final String configurableTablesString = "[sequence, variation, feature, alignment, reference, alignment_member, var_almt_note]";
	
	public static ServerRuntime createMetaRuntime(DatabaseConfiguration dbConfiguration, PropertiesConfiguration propertiesConfiguration) {
		return new ServerRuntime(META_DOMAIN_RESOURCE, dbConfigModule(dbConfiguration, propertiesConfiguration));
	}
	
	public static ServerRuntime createRootRuntime(DatabaseConfiguration dbConfiguration, PropertiesConfiguration propertiesConfiguration) {
		return new ServerRuntime(CORE_DOMAIN_RESOURCE, dbConfigModule(dbConfiguration, propertiesConfiguration));
	}

	public static String getDbSchemaVersionString(ObjectContext metaObjectContext) {
		SchemaVersion schemaVersion = lookup(metaObjectContext, SchemaVersion.class, SchemaVersion.pkMap(1), true);
		if(schemaVersion != null) {
			return schemaVersion.getSchemaVersion();
		} else {
			return "0";
		}
	}

	public static void setDbSchemaVersionString(ObjectContext metaObjectContext, String versionString) {
		SchemaVersion schemaVersion = lookup(metaObjectContext, SchemaVersion.class, SchemaVersion.pkMap(1), true);
		if(schemaVersion == null) {
			schemaVersion = create(metaObjectContext, SchemaVersion.class, SchemaVersion.pkMap(1), false);
		}
		schemaVersion.setSchemaVersion(versionString);
		
	}


	private static Module dbConfigModule(DatabaseConfiguration dbConfiguration, PropertiesConfiguration propertiesConfiguration) {
		String cacheSize = propertiesConfiguration.getPropertyValue(Constants.QUERY_CACHE_SIZE_PROPERTY);
		String cacheSizeFinal;
		if(cacheSize == null) {
			cacheSizeFinal = "20000";
		} else {
			cacheSizeFinal = cacheSize;
		}
	    String jdbcDriverClass = dbConfiguration.getVendor().getJdbcDriverClass();
		String jdbcUrl = dbConfiguration.getJdbcUrl();
	    Optional<String> username = dbConfiguration.getUsername();
	    Optional<String> password = dbConfiguration.getPassword();

	    return CayenneUtils.createCayenneDbConfigModule(cacheSizeFinal, jdbcDriverClass,
				jdbcUrl, username, password);
	}

	public static ServerRuntime createProjectModel(GluetoolsEngine gluetoolsEngine, Project project) {
		
		DatabaseConfiguration dbConfiguration = gluetoolsEngine.getDbConfiguration();
		PropertiesConfiguration propertiesConfiguration = gluetoolsEngine.getPropertiesConfiguration();

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
		
		Map<ConfigurableTable, List<Field>> cTableToFields = new LinkedHashMap<ConfigurableTable, List<Field>>();

		for(ConfigurableTable cTable : ConfigurableTable.values()) {
			cTableToFields.put(cTable, new LinkedList<Field>());
		}
		
		for(Field f: fields) {
			ConfigurableTable cTable = ConfigurableTable.valueOf(f.getTable());
			cTableToFields.get(cTable).add(f);
		}
		
		
		for(ConfigurableTable cTable : ConfigurableTable.values()) {
			List<Field> cTableFields = cTableToFields.get(cTable);
			{
				XPathExpression xPathExpression = 
						GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-entity[@name='"+cTable.name()+"']");
				Element sequenceTableElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocument, xPathExpression);
				cTableFields.forEach(f -> {
					Element dbAttributeElem = GlueXmlUtils.appendElementNS(sequenceTableElem, CAYENNE_NS, "db-attribute");
					dbAttributeElem.setAttribute("name", f.getName());
					dbAttributeElem.setAttribute("type", f.getFieldType().cayenneType());
					Optional.ofNullable(f.getMaxLength()).ifPresent(
							len -> { dbAttributeElem.setAttribute("length", Integer.toString(len)); });
				});
			}
			{
				XPathExpression xPathExpression = 
						GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:obj-entity[@name='"+cTable.getDataObjectClass().getSimpleName()+"']");
				Element sequenceObjElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocument, xPathExpression);
				cTableFields.forEach(f -> {
					Element objAttributeElem = GlueXmlUtils.appendElementNS(sequenceObjElem, CAYENNE_NS, "obj-attribute");
					objAttributeElem.setAttribute("name", f.getName());
					objAttributeElem.setAttribute("db-attribute-path", f.getName());
					objAttributeElem.setAttribute("type", f.getFieldType().javaType());
				});
			}
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
					    dbConfigModule(dbConfiguration, propertiesConfiguration),
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


	
	public static void deleteProjectModel(GluetoolsEngine gluetoolsEngine, Project project) {
		ServerRuntime projectRuntime = null;
		try {
			projectRuntime = createProjectModel(gluetoolsEngine, project);
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
	
	public static void addTableColumnToModel(GluetoolsEngine gluetoolsEngine, Project project, Field field) {
		ServerRuntime projectRuntime = null;
		try {
			projectRuntime = createProjectModel(gluetoolsEngine, project);
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
		DbEntity tableToModify = 
				mergerContext.getDataMap().getDbEntity(specializeTableName(field.getTable(), project.getName()));
		DbAttribute column = new DbAttribute(field.getName());
		column.setType(TypesMapping.getSqlTypeByName(field.getFieldType().cayenneType()));
		Integer maxLength = field.getMaxLength();
		if(maxLength != null) {
			column.setMaxLength(maxLength);
		}
		column.setEntity(tableToModify);
		return new AddColumnToDb(tableToModify, column);
	}

	private static MergerContext getMergerContext(Project project,
			ServerRuntime projectRuntime) {
		DataDomain domain = projectRuntime.getDataDomain();
		DataNode node = domain.getDataNode("glueproject-node");
		DataMap map = domain.getDataMap(projectMapName(project.getName()));
		MergerContext mergerContext = new ExecutingMergerContext(map, node);
		return mergerContext;
	}

	public static void deleteTableColumnFromModel(GluetoolsEngine gluetoolsEngine, Project project, Field field) {
		ServerRuntime projectRuntime = null;
		try {
			projectRuntime = createProjectModel(gluetoolsEngine, project);
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

	
	
	@SuppressWarnings("unused")
	private static <C extends GlueDataObject> C lookup(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap) {
		return lookup(objContext, objClass, pkMap, false);
	}
	
	private static <C extends GlueDataObject> C lookup(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap, 
			boolean allowNull) {
		Expression qualifier = pkMapToExpression(pkMap);
		return lookupFromDB(objContext, objClass, allowNull, qualifier);
	}

	private static <C extends GlueDataObject> C lookupFromDB(
			ObjectContext objContext, Class<C> objClass, boolean allowNull,
			Expression qualifier) {
		SelectQuery query = new SelectQuery(objClass, qualifier);
		List<?> results = objContext.performQuery(query);
		if(results.isEmpty()) {
			if(allowNull) {
				return null;
			} else {
				throw new DataModelException(DataModelException.Code.OBJECT_NOT_FOUND, objClass.getSimpleName(), qualifier.toString());
			}
		}
		if(results.size() > 1) {
			throw new DataModelException(DataModelException.Code.MULTIPLE_OBJECTS_FOUND, objClass.getSimpleName(), qualifier.toString());
		}
		C object = objClass.cast(results.get(0));
		return object;
	}

	private static Expression pkMapToExpression(Map<String, String> pkMap) {
		List<Expression> exps = pkMap.entrySet().stream().map(e -> 
			ExpressionFactory.matchExp(e.getKey(), e.getValue())).collect(Collectors.toList());
		Optional<Expression> exp = exps.stream().reduce(Expression::andExp);
		Expression qualifier = exp.get();
		return qualifier;
	}

	@SuppressWarnings("unused")
	private static <C extends GlueDataObject> DeleteResult delete(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap, 
			boolean allowNull) {
		C object = lookup(objContext, objClass, pkMap, allowNull);
		if(object != null) {
			try {
				objContext.deleteObject(object);
			} catch(DeleteDenyException dde) {
				String relationship = dde.getRelationship();
				throw new DataModelException(dde, DataModelException.Code.DELETE_DENIED, objClass.getSimpleName(), pkMap, relationship);
			}
			return new DeleteResult(objClass, 1);
		} else {
			return new DeleteResult(objClass, 0);
		}

	}

	
	@SuppressWarnings("unused")
	private static <C extends GlueDataObject> List<C> query(ObjectContext objContext, Class<C> objClass, SelectQuery query) {
		// should this also interact with the cache?
		List<?> queryResult = objContext.performQuery(query);
		return queryResult.stream().map(obj -> { 
			C dataObject = objClass.cast(obj);
			// ((GlueDataObject) dataObject).setFinalized();
			return dataObject;
		}).collect(Collectors.toList());
	}
	
	private static <C extends GlueDataObject> C create(ObjectContext objContext, Class<C> objClass, Map<String, String> pkMap, 
			boolean allowExists) {
		C existing = lookup(objContext, objClass, pkMap, true);
		if(existing != null) {
			if(allowExists) {
				return existing;
			} else {
				throw new DataModelException(DataModelException.Code.OBJECT_ALREADY_EXISTS, objClass.getSimpleName(), pkMap);
			}
		}
		C newObject = objContext.newObject(objClass);
		newObject.setPKValues(pkMap);
		return newObject;
	}

	
	
	
}
