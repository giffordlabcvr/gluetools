package uk.ac.gla.cvr.gluetools.core.command.project;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.resource.ResourceLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.ListCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.field.ListFieldsCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.resource.GlueResourceLocator;
import uk.ac.gla.cvr.gluetools.core.resource.GlueResourceMap;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class ProjectMode extends CommandMode {

	private static String CAYENNE_NS = "http://cayenne.apache.org/schema/3.0/modelMap";
	
	private String projectName;
	
	public ProjectMode(CommandContext cmdContext, String projectName) {
		super("project-"+projectName, PluginFactory.get(ProjectModeCommandFactory.creator));
		this.projectName = projectName;
		createProjectServerRuntime(cmdContext, projectName);
	}

	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass, Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(ProjectModeCommand.class.isAssignableFrom(cmdClass)) {
			Element projectNameElem = (Element) elem.appendChild(elem.getOwnerDocument().createElement("projectName"));
			projectNameElem.appendChild(elem.getOwnerDocument().createTextNode(projectName));
		}
	}

	private void createProjectServerRuntime(CommandContext cmdContext, String projectName) {
		Element listFieldsElem = CommandUsage.docElemForCmdClass(ListFieldsCommand.class);
		XmlUtils.appendElementWithText(listFieldsElem, "projectName", projectName);
		Expression exp = ExpressionFactory.matchExp(Field.PROJECT_PROPERTY, projectName);
		ListCommandResult<Field> listFieldsResult = CommandUtils.runListCommand(cmdContext, Field.class, new SelectQuery(Field.class, exp));
		List<Field> fields = listFieldsResult.getResults();
		Document cayenneDomainDocument;
		try(InputStream domainInputStream = getClass().getResourceAsStream("/"+CAYENNE_DOMAIN_RESOURCE)) {
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
		try(InputStream domainInputStream = getClass().getResourceAsStream("/"+CAYENNE_MAP_RESOURCE)) {
			cayenneMapDocument = XmlUtils.documentFromStream(domainInputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
		
		Element dataMapElem = cayenneMapDocument.getDocumentElement();
		Element firstDBEntityElem = XmlUtils.findChildElements(dataMapElem, "db-entity").get(0);
		Element newDBEntityElem = cayenneMapDocument.createElementNS(CAYENNE_NS, "db-entity");
		dataMapElem.insertBefore(newDBEntityElem, firstDBEntityElem);
		newDBEntityElem.setAttribute("name", "SEQ_FIELD_VALUES_"+projectName);
		Element sourceElem = XmlUtils.appendElement(newDBEntityElem, CAYENNE_NS, "db-attribute");
		sourceElem.setAttribute("name", "SOURCE");
		sourceElem.setAttribute("type", "VARCHAR");
		sourceElem.setAttribute("length", "50");
		sourceElem.setAttribute("isMandatory", "true");
		sourceElem.setAttribute("isPrimaryKey", "true");
		Element seqIdElem = XmlUtils.appendElement(newDBEntityElem, CAYENNE_NS, "db-attribute");
		seqIdElem.setAttribute("name", "SEQUENCE_ID");
		seqIdElem.setAttribute("type", "VARCHAR");
		seqIdElem.setAttribute("length", "50");
		seqIdElem.setAttribute("isMandatory", "true");
		seqIdElem.setAttribute("isPrimaryKey", "true");
		fields.forEach(f -> {
			Element dbAttributeElem = XmlUtils.appendElement(newDBEntityElem, CAYENNE_NS, "db-attribute");
			dbAttributeElem.setAttribute("name", f.getName());
			dbAttributeElem.setAttribute("type", f.getFieldType().name());
			Optional.ofNullable(f.getMaxLength()).ifPresent(
					len -> { dbAttributeElem.setAttribute("length", Integer.toString(len)); });
		});
		
		GlueResourceMap.getInstance().put("/"+projectDomainName, XmlUtils.prettyPrint(cayenneDomainDocument));
		GlueResourceMap.getInstance().put("/"+projectMapName+".map.xml", XmlUtils.prettyPrint(cayenneMapDocument));

//		XmlUtils.prettyPrint(cayenneDomainDocument, System.out);
//		XmlUtils.prettyPrint(cayenneMapDocument, System.out);

		ServerRuntime projectRuntime = new ServerRuntime(
					    projectDomainName, 
					     binder -> binder.bind(ResourceLocator.class)
					                     .to(GlueResourceLocator.class));
		// ensure it is working by getting the obj context.
		projectRuntime.getContext();
		setCayenneServerRuntime(projectRuntime);
	}

	
}
