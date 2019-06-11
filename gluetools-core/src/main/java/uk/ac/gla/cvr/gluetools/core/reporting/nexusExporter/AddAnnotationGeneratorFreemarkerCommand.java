package uk.ac.gla.cvr.gluetools.core.reporting.nexusExporter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleUpdateDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"add", "annotation-generator", "freemarker"}, 
		description = "Add a new annotation generator based on freemarker", 
		docoptUsages={"<annotationName> <freemarkerTemplate>"},
		docoptOptions={},
		metaTags = {CmdMeta.updatesDatabase}, 
		furtherHelp = ""
)
public class AddAnnotationGeneratorFreemarkerCommand extends ModuleDocumentCommand<OkResult> implements ModuleUpdateDocumentCommand {

	private String annotationName;
	private String freemarkerTemplate;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		annotationName = PluginUtils.configureStringProperty(configElem, "annotationName", true);
		freemarkerTemplate = PluginUtils.configureStringProperty(configElem, "freemarkerTemplate", true);
	}

	@Override
	protected OkResult processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		Element documentElement = modulePluginDoc.getDocumentElement();
		List<Element> childElements = new ArrayList<Element>(GlueXmlUtils.findChildElements(documentElement));
		childElements.forEach(elem -> {
			String elemAnnotationName = GlueXmlUtils.getXPathString(elem, "annotationName/text()");
			if(elemAnnotationName != null && elemAnnotationName.equals(annotationName)) {
				documentElement.removeChild(elem);
			}
		});
		Element freemarkerAnnotationGeneratorElem = GlueXmlUtils.appendElement(documentElement, "freemarkerAnnotationGenerator");
		GlueXmlUtils.appendElementWithText(freemarkerAnnotationGeneratorElem, "annotationName", annotationName);
		GlueXmlUtils.appendElementWithText(freemarkerAnnotationGeneratorElem, "template", freemarkerTemplate);
		return new OkResult();
	}
	

}

