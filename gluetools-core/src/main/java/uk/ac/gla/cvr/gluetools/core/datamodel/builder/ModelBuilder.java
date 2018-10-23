/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.datamodel.builder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.merge.AddColumnToDb;
import org.apache.cayenne.merge.CreateTableToDb;
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

import uk.ac.gla.cvr.gluetools.bcelgenerated.CustomTableObjectClassCreator;
import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.config.DatabaseConfiguration;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilderException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link.Multiplicity;
import uk.ac.gla.cvr.gluetools.core.datamodel.meta.SchemaVersion;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.PkField;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
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
	
	public static abstract class ModePathElement {

		public abstract String correctForm();
	}
	public static class Keyword extends ModePathElement {
		private String keyword;
		public Keyword(String keyword) {
			super();
			this.keyword = keyword;
		}
		public String getKeyword() {
			return keyword;
		}
		@Override
		public String correctForm() {
			return keyword;
		}
	}
	static Keyword keyword(String keyword) { return new Keyword(keyword); }
	public static class PkPath extends ModePathElement {
		private String pkPath;
		public PkPath(String pkPath) {
			super();
			this.pkPath = pkPath;
		}
		public String getPkPath() {
			return pkPath;
		}
		@Override
		public String correctForm() {
			return "<"+pkPath+">";
		}
	}
	static PkPath pkPath(String pkPath) { return new PkPath(pkPath); }
	
	
	public static final String configurableTablesString = "[sequence, variation, feature, feature_location, alignment, reference, alignment_member, var_almt_note, member_floc_note]";
	
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
	    String jdbcDriverClass = dbConfiguration.getDriverClass();
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
		
		
		
		List<CustomTable> customTables = project.getCustomTables();
		
		
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
		
		Document cayenneMapDocument = getProjectMapDocument();
		XPath xPath = createNamespacingXpath();

		
		// ensure custom table classes are available to GLUE class loaders.
		for(CustomTable customTable: customTables) {
			String className = CustomTableObjectClassCreator.getFullClassName(projectName, customTable.getName());
			synchronized(gluetoolsEngine) {
				if(!gluetoolsEngine.containsClass(className)) {
					CustomTableObjectClassCreator classCreator = new CustomTableObjectClassCreator(projectName, customTable.getName());
					byte[] classBytes = classCreator.create();
					gluetoolsEngine.addClass(className, classBytes);
				}
			}
		}
		
		
		// Custom tables -- DB entities.
		XPathExpression dataMapXPathExpression = 
				GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map");
		Element dataMapElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocument, dataMapXPathExpression);

		addCustomTablesToDocument(cayenneMapDocument, project);
		ensureProjectTablePkFields(cayenneMapDocument, project);

		for(CustomTable customTable : customTables) {
			String tableName = customTable.getName();
			XPathExpression xPathExpression = 
					GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-entity[@name='"+tableName+"']");
				Element dbEntityElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocument, xPathExpression);
			addCustomDbAttributesForTable(project, dbEntityElem, tableName);
		}
		
		Map<String, String> tableNameToObjEntity = new LinkedHashMap<String, String>();
		for(ConfigurableTable cTable: ConfigurableTable.values()) {
			XPathExpression xPathExpression = 
					GlueXmlUtils.compileXPathExpression(xPath, 
							"/cay:data-map/cay:obj-entity[@className='"+project.getDataObjectClass(cTable.name()).getCanonicalName()+"']");
			tableNameToObjEntity.put(cTable.name(), 
					GlueXmlUtils.getXPathElement(cayenneMapDocument, xPathExpression).getAttribute("name"));
		}

		
		// Custom tables -- Object entities.
		for(CustomTable customTable : customTables) {
			Element objEntityElem = GlueXmlUtils.appendElementNS(dataMapElem, CAYENNE_NS, "obj-entity");
			String tableName = customTable.getName();
			String objEntityName = tableName;
			tableNameToObjEntity.put(tableName, objEntityName);
			objEntityElem.setAttribute("name", objEntityName);
			String customTableClassName = CustomTableObjectClassCreator.getFullClassName(projectName, tableName);
			objEntityElem.setAttribute("className", customTableClassName);
			objEntityElem.setAttribute("dbEntityName", tableName);
			objEntityElem.setAttribute("superClassName", CustomTableObject.class.getCanonicalName());
			Element idAttributeElem = GlueXmlUtils.appendElementNS(objEntityElem, CAYENNE_NS, "obj-attribute");
			idAttributeElem.setAttribute("name", "id");
			idAttributeElem.setAttribute("db-attribute-path", "id");
			idAttributeElem.setAttribute("type", FieldType.VARCHAR.javaType());
			addCustomObjAttributesForTable(project, objEntityElem, tableName);

		}

		for(ConfigurableTable cTable : ConfigurableTable.values()) {
			String tableName = cTable.name();
			// Configurable tables -- DB entities.
			{ 
				XPathExpression xPathExpression = 
						GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-entity[@name='"+tableName+"']");
					Element dbEntityElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocument, xPathExpression);
				addCustomDbAttributesForTable(project, dbEntityElem, tableName);
			}
			// Configurable tables -- Obj entities.
			{
				XPathExpression xPathExpression = 
						GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:obj-entity[@name='"+cTable.getDataObjectClass().getSimpleName()+"']");
				Element objEntityElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocument, xPathExpression);
				addCustomObjAttributesForTable(project, objEntityElem, tableName);
			}
			project.setClassTableName(cTable.dataObjectClass, tableName);
		}
		
		// add DB relationships in both directions for each link.
		for(Link link: project.getLinks()) {
			String srcTableName = link.getSrcTableName();
			String srcLinkName = link.getSrcLinkName();
			String destTableName = link.getDestTableName();
			String destLinkName = link.getDestLinkName();
			Element dbRelSrcToDestElem = GlueXmlUtils.appendElementNS(dataMapElem, CAYENNE_NS, "db-relationship");
			dbRelSrcToDestElem.setAttribute("name", srcLinkName);
			dbRelSrcToDestElem.setAttribute("source", srcTableName);
			dbRelSrcToDestElem.setAttribute("target", destTableName);
			if(link.isToMany()) {
				dbRelSrcToDestElem.setAttribute("toMany", "true");
				// table on ONE side of ONE_TO_MANY relationship does not have specific field.
				for(PkField srcPkField : project.getTablePkFields(srcTableName)) {
					Element dbAttrPairElem = GlueXmlUtils.appendElementNS(dbRelSrcToDestElem, CAYENNE_NS, "db-attribute-pair");
					dbAttrPairElem.setAttribute("source", srcPkField.getName());
					dbAttrPairElem.setAttribute("target", dbAttributeNameForLinkPK(destLinkName, srcPkField.getName()));
				}
			} else {
				dbRelSrcToDestElem.setAttribute("toMany", "false");
				// one-to-one or many-to-one
				for(PkField destPkField : project.getTablePkFields(destTableName)) {
					Element dbAttrPairElem = GlueXmlUtils.appendElementNS(dbRelSrcToDestElem, CAYENNE_NS, "db-attribute-pair");
					dbAttrPairElem.setAttribute("source", dbAttributeNameForLinkPK(srcLinkName, destPkField.getName()));
					dbAttrPairElem.setAttribute("target", destPkField.getName());
				}
			}
			Element dbRelDestToSrcElem = GlueXmlUtils.appendElementNS(dataMapElem, CAYENNE_NS, "db-relationship");
			dbRelDestToSrcElem.setAttribute("name", destLinkName);
			dbRelDestToSrcElem.setAttribute("source", destTableName);
			dbRelDestToSrcElem.setAttribute("target", srcTableName);
			if(link.isFromMany()) {
				dbRelDestToSrcElem.setAttribute("toMany", "true");
				// table on ONE side of MANY_TO_ONE relationship does not have specific field.
				for(PkField destPkField : project.getTablePkFields(destTableName)) {
					Element dbAttrPairElem = GlueXmlUtils.appendElementNS(dbRelDestToSrcElem, CAYENNE_NS, "db-attribute-pair");
					dbAttrPairElem.setAttribute("source", destPkField.getName());
					dbAttrPairElem.setAttribute("target", dbAttributeNameForLinkPK(srcLinkName, destPkField.getName()));
				}
			} else {
				dbRelDestToSrcElem.setAttribute("toMany", "false");
				for(PkField srcPkField : project.getTablePkFields(srcTableName)) {
					Element dbAttrPairElem = GlueXmlUtils.appendElementNS(dbRelDestToSrcElem, CAYENNE_NS, "db-attribute-pair");
					dbAttrPairElem.setAttribute("source", dbAttributeNameForLinkPK(destLinkName, srcPkField.getName()));
					dbAttrPairElem.setAttribute("target", srcPkField.getName());
				}
			}
		}

		// add Obj relationships in both directions for each link.
		for(Link link: project.getLinks()) {
			String srcTableName = link.getSrcTableName();
			String srcLinkName = link.getSrcLinkName();
			String destTableName = link.getDestTableName();
			String destLinkName = link.getDestLinkName();
			Element objRelSrcToDestElem = GlueXmlUtils.appendElementNS(dataMapElem, CAYENNE_NS, "obj-relationship");
			objRelSrcToDestElem.setAttribute("name", srcLinkName);
			objRelSrcToDestElem.setAttribute("source", tableNameToObjEntity.get(srcTableName));
			objRelSrcToDestElem.setAttribute("target", tableNameToObjEntity.get(destTableName));
			objRelSrcToDestElem.setAttribute("deleteRule", "Nullify");
			objRelSrcToDestElem.setAttribute("db-relationship-path", srcLinkName);

			Element objRelDestToSrcElem = GlueXmlUtils.appendElementNS(dataMapElem, CAYENNE_NS, "obj-relationship");
			objRelDestToSrcElem.setAttribute("name", destLinkName);
			objRelDestToSrcElem.setAttribute("source", tableNameToObjEntity.get(destTableName));
			objRelDestToSrcElem.setAttribute("target", tableNameToObjEntity.get(srcTableName));
			objRelDestToSrcElem.setAttribute("deleteRule", "Nullify");
			objRelDestToSrcElem.setAttribute("db-relationship-path", destLinkName);
		}
		
		// specialize various names so that they are specific to the project
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

		// GlueXmlUtils.prettyPrint(cayenneDomainDocument, System.out);
		// GlueXmlUtils.prettyPrint(cayenneMapDocument, System.out);

		ServerRuntime projectRuntime = null;
		projectRuntime = new ServerRuntime(
				projectDomainName, 
				dbConfigModule(dbConfiguration, propertiesConfiguration),
				binder -> binder.bind(ResourceLocator.class)
				.to(GlueResourceLocator.class));
		EntityResolver entityResolver = projectRuntime.getContext().getEntityResolver();
		for(CustomTable customTable: customTables) {
			// ensure all entity classes are loaded, and associated with table objects.
			@SuppressWarnings("unchecked")
			Class<? extends CustomTableObject> customTableRowClass =
					(Class<? extends CustomTableObject>) entityResolver
					.getClassDescriptor(customTable.getName()).getObjectClass();
			customTable.setRowObjectClass(customTableRowClass);
			project.setClassTableName(customTableRowClass, customTable.getName());
		}
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

	private static void ensureProjectTablePkFields(Document cayenneMapDocument,
			Project project) {
		for(String tableName: project.getTableNames()) {
			project.setTablePkFields(tableName, tableNameToPkFields(cayenneMapDocument, tableName));
		}
	}

	private static void addCustomTablesToDocument(Document cayenneMapDocument, Project project) {
		XPath xPath = createNamespacingXpath();
		XPathExpression dataMapXPathExpression = 
				GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map");
		Element dataMapElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocument, dataMapXPathExpression);

		for(CustomTable customTable : project.getCustomTables()) {
			Element dbEntityElem = GlueXmlUtils.appendElementNS(dataMapElem, CAYENNE_NS, "db-entity");
			String tableName = customTable.getName();
			dbEntityElem.setAttribute("name", tableName);
			Element idAttributeElem = GlueXmlUtils.appendElementNS(dbEntityElem, CAYENNE_NS, "db-attribute");
			idAttributeElem.setAttribute("name", "id");
			idAttributeElem.setAttribute("type", "VARCHAR");
			idAttributeElem.setAttribute("isPrimaryKey", "true");
			idAttributeElem.setAttribute("isMandatory", "true");
			idAttributeElem.setAttribute("length", Integer.toString(customTable.getIdFieldLength()));
		}
	}

	private static void addCustomObjAttributesForTable(Project project,
			Element objEntityElem, String tableName) {
		List<Field> customTableFields = project.getCustomFields(tableName);
		customTableFields.forEach(f -> {
			Element objAttributeElem = GlueXmlUtils.appendElementNS(objEntityElem, CAYENNE_NS, "obj-attribute");
			objAttributeElem.setAttribute("name", f.getName());
			objAttributeElem.setAttribute("db-attribute-path", f.getName());
			objAttributeElem.setAttribute("type", f.getFieldType().javaType());
		});
	}

	private static void addCustomDbAttributesForTable(Project project, Element dbEntityElem, String tableName) {
		List<Field> customFields = project.getCustomFields(tableName);
		customFields.forEach(f -> {
			Element dbAttributeElem = GlueXmlUtils.appendElementNS(dbEntityElem, CAYENNE_NS, "db-attribute");
			dbAttributeElem.setAttribute("name", f.getName());
			dbAttributeElem.setAttribute("type", f.getFieldType().cayenneType());
			Optional.ofNullable(f.getMaxLength()).ifPresent(
					len -> { dbAttributeElem.setAttribute("length", Integer.toString(len)); });
		});
		List<Link> linksForWhichSource = project.getLinksForWhichSource(tableName);
		linksForWhichSource.forEach(l -> {
			for(PkField pkField: linkFieldsForSrcTable(project, l)) {
				Element linkAttrElem = GlueXmlUtils.appendElementNS(dbEntityElem, CAYENNE_NS, "db-attribute");
				linkAttrElem.setAttribute("name", pkField.getName());
				linkAttrElem.setAttribute("type", pkField.getCayenneType());
				Integer maxLength = pkField.getMaxLength();
				if(maxLength != null) {
					linkAttrElem.setAttribute("length", Integer.toString(maxLength));
				}
			}
		});
		List<Link> linksForWhichDest = project.getLinksForWhichDestination(tableName);
		linksForWhichDest.forEach(l -> {
			for(PkField pkField: linkFieldsForDestTable(project, l)) {
				Element linkAttrElem = GlueXmlUtils.appendElementNS(dbEntityElem, CAYENNE_NS, "db-attribute");
				linkAttrElem.setAttribute("name", pkField.getName());
				linkAttrElem.setAttribute("type", pkField.getCayenneType());
				Integer maxLength = pkField.getMaxLength();
				if(maxLength != null) {
					linkAttrElem.setAttribute("length", Integer.toString(maxLength));
				}
			}
		});
	}

	private static String dbAttributeNameForLinkPK(String linkName, String pkName) {
		return linkName+"_"+pkName;
	}

	private static Document getProjectMapDocument() {
		Document cayenneMapDocument;
		try(InputStream domainInputStream = ModelBuilder.class.getResourceAsStream("/"+PROJECT_MAP_RESOURCE)) {
			cayenneMapDocument = GlueXmlUtils.documentFromStream(domainInputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
		return cayenneMapDocument;
	}

	private static XPath createNamespacingXpath() {
		XPath xPath = GlueXmlUtils.createXPathEngine();
		XmlNamespaceContext namespaceContext = new GlueXmlUtils.XmlNamespaceContext();
		namespaceContext.addNamespace("cay", CAYENNE_NS);
		xPath.setNamespaceContext(namespaceContext);
		return xPath;
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

	public static String specializeTableName(String tableName, String projectName) {
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
	
	public static void addFieldToModel(GluetoolsEngine gluetoolsEngine, Project project, Field field) {
		validateAddField(gluetoolsEngine, project, field);
		ServerRuntime projectRuntime = null;
		try {
			projectRuntime = createProjectModel(gluetoolsEngine, project);
			MergerContext mergerContext = getMergerContext(project, projectRuntime);
			AddColumnToDb addToken = getAddColumnToken(project, mergerContext, field.getTable(), field.getName(), field.getFieldType().cayenneType(), field.getMaxLength());
			addToken.execute(mergerContext);
		} finally {
			if(projectRuntime != null) {
				projectRuntime.shutdown();
			}
		}
	}
	
	public static void addCustomTableToModel(GluetoolsEngine gluetoolsEngine, Project project, CustomTable customTable) {
		validateAddCustomTable(gluetoolsEngine, project, customTable);
		ServerRuntime projectRuntime = null;
		try {
			projectRuntime = createProjectModel(gluetoolsEngine, project);
			MergerContext mergerContext = getMergerContext(project, projectRuntime);
			CreateTableToDb createTableToken = getCreateTableToken(project, customTable, mergerContext);
			createTableToken.execute(mergerContext);
		} finally {
			if(projectRuntime != null) {
				projectRuntime.shutdown();
			}
		}
	}

	private static void validateAddCustomTable(GluetoolsEngine gluetoolsEngine, Project project, CustomTable customTable) {
		String tableName = customTable.getName();
		// Check that custom table name does not overlap existing custom table
		for(CustomTable existingCustomTable: project.getCustomTables()) {
			if(existingCustomTable.getName().equals(customTable.getName())) {
				throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom table with name '"+tableName+"': this is used by an existing custom table");
			}
		}
		// Check that custom table name does not overlap core DB entity.
		XPath xPath = createNamespacingXpath();
		Document cayenneMapDocument = getProjectMapDocument();
		XPathExpression xPathExpression = 
				GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-entity[@name='"+tableName+"']");
		Element dbEntityElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocument, xPathExpression);
		if(dbEntityElem != null) {
			throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom table with name '"+tableName+"': this is used by a core GLUE DB entity");
		}
	}

	private static void validateDeleteCustomTable(GluetoolsEngine gluetoolsEngine, Project project, CustomTable customTable) {
		String tableName = customTable.getName();
		if(!project.getLinksForWhichSource(tableName).isEmpty()) {
			throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Custom table '"+tableName+"' cannot be deleted as it is the source table of a custom relational link");
		}
		if(!project.getLinksForWhichDestination(tableName).isEmpty()) {
			throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Custom table '"+tableName+"' cannot be deleted as it is the destination table of a custom relational link");
		}
	}

	private static void validateAddField(GluetoolsEngine gluetoolsEngine, Project project, Field field) {
		String fieldName = field.getName();
		String tableName = field.getTable();
		for(Field existingField: project.getCustomFields(tableName)) {
			if(existingField.getName().equals(fieldName)) {
				throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom field with name '"+fieldName+"': this is used by an existing custom field on table '"+tableName+"'");
			}
		}
		for(Link existingLink: project.getLinksForWhichSource(tableName)) {
			if(existingLink.getSrcLinkName().equals(fieldName)) {
				throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom field with name '"+fieldName+"': this is used as the source link name by a custom link with source table '"+tableName+"'");
			}
		}
		for(Link existingLink: project.getLinksForWhichDestination(tableName)) {
			if(existingLink.getDestLinkName().equals(fieldName)) {
				throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom field with name '"+fieldName+"': this is used as the destination link name by a custom link with destination table '"+tableName+"'");
			}
		}
		if(project.getCustomTable(tableName) != null && fieldName.equals(CustomTableObject.ID_PROPERTY)) {
			throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom field with name '"+fieldName+"': this is already used as the primary key for '"+tableName+"'");
		} else {
			XPath xPath = createNamespacingXpath();
			Document cayenneMapDocument = getProjectMapDocument();
			XPathExpression xPathExpression = 
					GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-entity[@name='"+tableName+"']/cay:db-attribute[@name='"+fieldName+"']");
			Element dbAttributeElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocument, xPathExpression);
			if(dbAttributeElem != null) {
				throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom field with name '"+fieldName+"': this is used by an existing core field on table '"+tableName+"'");
			}
		}
	}

	private static void validateDeleteField(GluetoolsEngine gluetoolsEngine, Project project, Field field) {
	}

	private static void validateAddLink(GluetoolsEngine gluetoolsEngine, Project project, Link link) {
		
		Document cayenneMapDocumentExtended = getProjectMapDocument();
		addCustomTablesToDocument(cayenneMapDocumentExtended, project);
		ensureProjectTablePkFields(cayenneMapDocumentExtended, project);

		String srcTableName = link.getSrcTableName();
		String srcLinkName = link.getSrcLinkName();
		String destTableName = link.getDestTableName();
		String destLinkName = link.getDestLinkName();
		for(Link existingLink: project.getLinks()) {
			if(srcTableName.equals(existingLink.getSrcTableName()) && srcLinkName.equals(existingLink.getSrcLinkName())) {
				throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom link with source table '"+srcTableName+"' and source link name '"+srcLinkName+"': such a link already exists");
			}
			if(destTableName.equals(existingLink.getDestTableName()) && destLinkName.equals(existingLink.getDestLinkName())) {
				throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom link with destination table '"+destTableName+"' and destination link name '"+destLinkName+"': such a link already exists");
			}
		}
		XPath xPath = createNamespacingXpath();
		for(PkField pkField : linkFieldsForSrcTable(project, link)) {
			XPathExpression xPathExpression = 
					GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-entity[@name='"+srcTableName+"']/cay:db-attribute[@name='"+pkField.getName()+"']");
			Element dbAttributeElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocumentExtended, xPathExpression);
			if(dbAttributeElem != null) {
				throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom link with source table '"+srcTableName+"' and source link name '"+srcLinkName+"': because a core field on this table named '"+pkField.getName()+"' already exists");
			}
			for(Field existingField: project.getCustomFields(srcTableName)) {
				String existingFieldName = existingField.getName();
				if(existingFieldName.equals(pkField.getName())) {
					throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom link with source table '"+srcTableName+"' and source link name '"+srcLinkName+"', because a custom field on this table named '"+existingFieldName+"' already exists");
				}
			}
		}
		for(PkField pkField : linkFieldsForDestTable(project, link)) {
			XPathExpression xPathExpression = 
					GlueXmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-entity[@name='"+destTableName+"']/cay:db-attribute[@name='"+pkField.getName()+"']");
			Element dbAttributeElem = (Element) GlueXmlUtils.getXPathNode(cayenneMapDocumentExtended, xPathExpression);
			if(dbAttributeElem != null) {
				throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom link with destination table '"+destTableName+"' and destination link name '"+srcLinkName+"': because a core field on this table named '"+pkField.getName()+"' already exists");
			}
			for(Field existingField: project.getCustomFields(destTableName)) {
				String existingFieldName = existingField.getName();
				if(existingFieldName.equals(pkField.getName())) {
					throw new ModelBuilderException(Code.INVALID_SCHEMA_CHANGE, "Cannot add a custom link with destination table '"+destTableName+"' and destination link name '"+destLinkName+"', because a custom field on this table named '"+existingFieldName+"' already exists");
				}
			}
		}
	}

	private static void validateDeleteLink(GluetoolsEngine gluetoolsEngine, Project project, Link link) {
	}

	
	public static void deleteCustomTableFromModel(GluetoolsEngine gluetoolsEngine, Project project, CustomTable customTable) {
		validateDeleteCustomTable(gluetoolsEngine, project, customTable);
		ServerRuntime projectRuntime = null;
		try {
			projectRuntime = createProjectModel(gluetoolsEngine, project);
			MergerContext mergerContext = getMergerContext(project, projectRuntime);
			DropTableToDb dropTableToken = getDropTableToken(project, customTable, mergerContext);
			dropTableToken.execute(mergerContext);
		} finally {
			if(projectRuntime != null) {
				projectRuntime.shutdown();
			}
		}
	}

	private static AddColumnToDb getAddColumnToken(Project project, MergerContext mergerContext, String tableName, String columnName, String cayenneType, Integer maxLength) {
		DbEntity tableToModify = 
				mergerContext.getDataMap().getDbEntity(specializeTableName(tableName, project.getName()));
		DbAttribute column = new DbAttribute(columnName);
		column.setType(TypesMapping.getSqlTypeByName(cayenneType));
		if(maxLength != null) {
			column.setMaxLength(maxLength);
		}
		column.setEntity(tableToModify);
		return new AddColumnToDb(tableToModify, column);
	}

	private static CreateTableToDb getCreateTableToken(Project project, CustomTable customTable, MergerContext mergerContext) {
		DbEntity dbEntity = new DbEntity(specializeTableName(customTable.getName(), project.getName()));
		DbAttribute idColumn = new DbAttribute("id");
		idColumn.setType(TypesMapping.getSqlTypeByName(FieldType.VARCHAR.cayenneType()));
		idColumn.setMandatory(true);
		idColumn.setMaxLength(customTable.getIdFieldLength());
		idColumn.setPrimaryKey(true);
		dbEntity.addAttribute(idColumn);
		return new CreateTableToDb(dbEntity);
	}

	private static DropTableToDb getDropTableToken(Project project, CustomTable customTable, MergerContext mergerContext) {
		DbEntity tableToDelete = 
				mergerContext.getDataMap().getDbEntity(specializeTableName(customTable.getName(), project.getName()));
		return new DropTableToDb(tableToDelete);
	}
	
	private static MergerContext getMergerContext(Project project,
			ServerRuntime projectRuntime) {
		DataDomain domain = projectRuntime.getDataDomain();
		DataNode node = domain.getDataNode("glueproject-node");
		DataMap map = domain.getDataMap(projectMapName(project.getName()));
		MergerContext mergerContext = new ExecutingMergerContext(map, node);
		return mergerContext;
	}

	public static void deleteFieldFromModel(GluetoolsEngine gluetoolsEngine, Project project, Field field) {
		validateDeleteField(gluetoolsEngine, project, field);
		ServerRuntime projectRuntime = null;
		try {
			projectRuntime = createProjectModel(gluetoolsEngine, project);
			MergerContext mergerContext = getMergerContext(project, projectRuntime);
			AddColumnToDb addToken = getAddColumnToken(project, mergerContext, field.getTable(), field.getName(), field.getFieldType().cayenneType(), field.getMaxLength());
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

	public static void addLinkToModel(GluetoolsEngine gluetoolsEngine, Project project, Link link) {
		validateAddLink(gluetoolsEngine, project, link);
		ServerRuntime projectRuntime = null;
		try {
			projectRuntime = createProjectModel(gluetoolsEngine, project);
			MergerContext mergerContext = getMergerContext(project, projectRuntime);
			List<AddColumnToDb> addTokens = addColumnTokensForLink(project, mergerContext, link);
			for(AddColumnToDb addToken: addTokens) {
				addToken.execute(mergerContext);
			}
			
		} finally {
			if(projectRuntime != null) {
				projectRuntime.shutdown();
			}
		}
	}

	private static List<AddColumnToDb> addColumnTokensForLink(Project project,
			MergerContext mergerContext, Link link) {
		List<AddColumnToDb> addTokens = new ArrayList<AddColumnToDb>();
		String srcTableName = link.getSrcTableName();
		String destTableName = link.getDestTableName();
		addTokens.addAll(linkFieldsForSrcTable(project, link)
				.stream()
				.map(pkf -> 
					getAddColumnToken(project, mergerContext, srcTableName, pkf.getName(), pkf.getCayenneType(), pkf.getMaxLength()))
				.collect(Collectors.toList()));
		addTokens.addAll(linkFieldsForDestTable(project, link)
				.stream()
				.map(pkf -> 
					getAddColumnToken(project, mergerContext, destTableName, pkf.getName(), pkf.getCayenneType(), pkf.getMaxLength()))
				.collect(Collectors.toList()));
		return addTokens;
	}
	
	private static List<PkField> linkFieldsForSrcTable(Project project, Link link) {
		String destTableName = link.getDestTableName();
		String srcLinkName = link.getSrcLinkName();
		List<PkField> linkFields = new ArrayList<PkField>();
		if(!link.getMultiplicity().equals(Multiplicity.ONE_TO_MANY.name())) {
			List<PkField> destPkFields = project.getTablePkFields(destTableName);
			for(PkField destPkField: destPkFields) {
				String columnName = dbAttributeNameForLinkPK(srcLinkName, destPkField.getName());
				String columnType = destPkField.getCayenneType();
				Integer columnLength = destPkField.getMaxLength();
				linkFields.add(new PkField(columnName, columnType, columnLength));
			}
		}
		return linkFields;
	}

	private static List<PkField> linkFieldsForDestTable(Project project, Link link) {
		String srcTableName = link.getSrcTableName();
		String destLinkName = link.getDestLinkName();
		List<PkField> linkFields = new ArrayList<PkField>();
		if(!link.getMultiplicity().equals(Multiplicity.MANY_TO_ONE.name())) {
			List<PkField> srcPkFields = project.getTablePkFields(srcTableName);
			for(PkField srcPkField: srcPkFields) {
				String columnName = dbAttributeNameForLinkPK(destLinkName, srcPkField.getName());
				String columnType = srcPkField.getCayenneType();
				Integer columnLength = srcPkField.getMaxLength();
				linkFields.add(new PkField(columnName, columnType, columnLength));
			}
		}
		return linkFields;
	}

	public static void deleteLinkFromModel(GluetoolsEngine gluetoolsEngine, Project project, Link link) {
		validateDeleteLink(gluetoolsEngine, project, link);
		ServerRuntime projectRuntime = null;
		try {
			projectRuntime = createProjectModel(gluetoolsEngine, project);
			MergerContext mergerContext = getMergerContext(project, projectRuntime);
			List<AddColumnToDb> addTokens = addColumnTokensForLink(project, mergerContext, link);
			for(AddColumnToDb addToken: addTokens) {
				DropColumnToDb dropToken = new DropColumnToDb(addToken.getEntity(), addToken.getColumn());
				dropToken.execute(mergerContext);
			}
		} finally {
			if(projectRuntime != null) {
				projectRuntime.shutdown();
			}
		}

		
	}


	private static List<PkField> tableNameToPkFields(Document cayenneMapDocument, String tableName) {
		XPath xPath = createNamespacingXpath();
		XPathExpression xPathExpression = 
				GlueXmlUtils.compileXPathExpression(xPath, 
						"/cay:data-map/cay:db-entity[@name='"+tableName+"']/cay:db-attribute[@isPrimaryKey = 'true']");
		List<Element> pkElements = GlueXmlUtils.getXPathElements(cayenneMapDocument, xPathExpression);
		return pkElements.stream().map(pkElem -> {
			String columnName = pkElem.getAttribute("name");
			String columnType = pkElem.getAttribute("type");
			String columnLengthString = pkElem.getAttribute("length");
			Integer columnLength = null;
			if(columnLengthString != null && columnLengthString.length() > 0) {
				columnLength = Integer.parseInt(columnLengthString);
			}
			return new PkField(columnName, columnType, columnLength);
		}).collect(Collectors.toList());
	}

	
	
	
}
