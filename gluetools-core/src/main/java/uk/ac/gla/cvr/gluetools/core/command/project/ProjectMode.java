package uk.ac.gla.cvr.gluetools.core.command.project;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.resource.ResourceLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.ListCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.root.ListSequenceFieldsCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.resource.GlueResourceLocator;
import uk.ac.gla.cvr.gluetools.core.resource.GlueResourceMap;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils.XmlNamespaceContext;

public class ProjectMode extends CommandMode {

	public static String PROJECT_DOMAIN_RESOURCE = "cayenne-glueproject-domain.xml";
	public static String PROJECT_MAP_RESOURCE = "glueproject-map.map.xml";

	private static String CAYENNE_NS = "http://cayenne.apache.org/schema/3.0/modelMap";
	
	private String projectName;
	private Map<String, Field> fieldNameToField = new LinkedHashMap<String, Field>();
	
	public ProjectMode(CommandContext cmdContext, String projectName) {
		super("project-"+projectName, PluginFactory.get(ProjectModeCommandFactory.creator));
		this.projectName = projectName;
		createProjectServerRuntime(cmdContext, projectName);
	}

	private void createProjectServerRuntime(CommandContext cmdContext, String projectName) {
		Element listFieldsElem = CommandUsage.docElemForCmdClass(ListSequenceFieldsCommand.class);
		XmlUtils.appendElementWithText(listFieldsElem, "projectName", projectName);
		Expression exp = ExpressionFactory.matchExp(Field.PROJECT_PROPERTY, projectName);
		ListCommandResult<Field> listFieldsResult = CommandUtils.runListCommand(cmdContext, Field.class, new SelectQuery(Field.class, exp));
		List<Field> fields = listFieldsResult.getResults();
		Document cayenneDomainDocument;
		try(InputStream domainInputStream = getClass().getResourceAsStream("/"+PROJECT_DOMAIN_RESOURCE)) {
			cayenneDomainDocument = XmlUtils.documentFromStream(domainInputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
		String projectDomainName = "cayenne-project-"+projectName+"-domain.xml";
		String projectMapName = "project-"+projectName+"-map";
		Element domainElem = cayenneDomainDocument.getDocumentElement();
		XmlUtils.findChildElements(domainElem, "map").get(0).setAttribute("name", projectMapName);
		Element nodeElem = XmlUtils.findChildElements(domainElem, "node").get(0);
		XmlUtils.findChildElements(nodeElem, "map-ref").get(0).setAttribute("name", projectMapName);
		
		Document cayenneMapDocument;
		try(InputStream domainInputStream = getClass().getResourceAsStream("/"+PROJECT_MAP_RESOURCE)) {
			cayenneMapDocument = XmlUtils.documentFromStream(domainInputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
		XPath xPath = XmlUtils.createXPathEngine();
		XmlNamespaceContext namespaceContext = new XmlUtils.XmlNamespaceContext();
		namespaceContext.addNamespace("cay", CAYENNE_NS);
		xPath.setNamespaceContext(namespaceContext);
		{
			XPathExpression xPathExpression = 
					XmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-entity[@name='SEQUENCE']");
			Element sequenceTableElem = (Element) XmlUtils.getXPathNode(cayenneMapDocument, xPathExpression);
			fields.forEach(f -> {
				Element dbAttributeElem = XmlUtils.appendElement(sequenceTableElem, CAYENNE_NS, "db-attribute");
				dbAttributeElem.setAttribute("name", f.getName());
				dbAttributeElem.setAttribute("type", f.getFieldType().name());
				Optional.ofNullable(f.getMaxLength()).ifPresent(
						len -> { dbAttributeElem.setAttribute("length", Integer.toString(len)); });
			});
		}
		{
			XPathExpression xPathExpression = 
					XmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:obj-entity[@name='Sequence']");
			Element sequenceObjElem = (Element) XmlUtils.getXPathNode(cayenneMapDocument, xPathExpression);
			fields.forEach(f -> {
				Element objAttributeElem = XmlUtils.appendElement(sequenceObjElem, CAYENNE_NS, "obj-attribute");
				objAttributeElem.setAttribute("name", f.getName());
				objAttributeElem.setAttribute("db-attribute-path", f.getName());
				objAttributeElem.setAttribute("type", f.getFieldType().javaType());
			});
		}
		{
			XPathExpression xPathExpression = 
					XmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-relationship");
			XmlUtils.getXPathNodes(cayenneMapDocument, xPathExpression).forEach(n -> {
				Element dbRelationshipElem = (Element) n;
				specializeAttribute(projectName, dbRelationshipElem, "source");
				specializeAttribute(projectName, dbRelationshipElem, "target");
				
			});
		}
		{
			XPathExpression xPathExpression = 
					XmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:db-entity");
			XmlUtils.getXPathNodes(cayenneMapDocument, xPathExpression).forEach(n -> {
				Element dbRelationshipElem = (Element) n;
				specializeAttribute(projectName, dbRelationshipElem, "name");
				
			});
		}

		{
			XPathExpression xPathExpression = 
					XmlUtils.compileXPathExpression(xPath, "/cay:data-map/cay:obj-entity");
			XmlUtils.getXPathNodes(cayenneMapDocument, xPathExpression).forEach(n -> {
				Element dbRelationshipElem = (Element) n;
				specializeAttribute(projectName, dbRelationshipElem, "dbEntityName");
				
			});
		}

		
		GlueResourceMap.getInstance().put("/"+projectDomainName, XmlUtils.prettyPrint(cayenneDomainDocument));
		GlueResourceMap.getInstance().put("/"+projectMapName+".map.xml", XmlUtils.prettyPrint(cayenneMapDocument));

		XmlUtils.prettyPrint(cayenneDomainDocument, System.out);
		XmlUtils.prettyPrint(cayenneMapDocument, System.out);

		ServerRuntime projectRuntime = new ServerRuntime(
					    projectDomainName, 
					     binder -> binder.bind(ResourceLocator.class)
					                     .to(GlueResourceLocator.class));
		// ensure it is working by getting the obj context.
		projectRuntime.getContext();
		setCayenneServerRuntime(projectRuntime);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			System.out.println(getNameTablesInDB(projectRuntime.getDataDomain().getDataNode("glueproject-node")));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void specializeAttribute(String projectName, Element elem, String attrName) {
		elem.setAttribute(attrName, elem.getAttribute(attrName)+"_"+projectName);
	}

	public Field getSequenceField(String fieldName) {
		return fieldNameToField.get(fieldName);
	}

	public List<String> getSequenceFieldNames() {
		return new ArrayList<String>(fieldNameToField.keySet());
	}
	
	protected List<String> getNameTablesInDB(DataNode dataNode)
            throws SQLException {
        String tableLabel = dataNode.getAdapter().tableTypeForTable();
        Connection con = null;
        List<String> nameTables = new LinkedList<String>();
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

	
}
