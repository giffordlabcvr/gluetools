package uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.freemarker;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.ObjectRenderer;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

@PluginClass(elemName="freemarkerObjectRenderer")
public class FreemarkerObjectRenderer extends ObjectRenderer<FreemarkerObjectRenderer> {

	public static String TEMPLATE_FILE_NAME = "templateFileName";
	
	private String templateFileName;

	private Template template = null;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.templateFileName = PluginUtils.configureStringProperty(configElem, TEMPLATE_FILE_NAME, true);
		registerResourceName(templateFileName);
	}
	
	@Override
	public byte[] renderToXmlBytes(CommandContext cmdContext, GlueDataObject renderableObject) {
		if(template == null) {
			byte[] templateBytes = getResource(cmdContext, templateFileName);
			template = FreemarkerUtils.templateFromString(
					// xml extension triggers xml escaping
					getModuleName()+":"+templateFileName, new String(templateBytes),  
						cmdContext.getGluetoolsEngine().getFreemarkerConfiguration());
		}
		TemplateModel templateModel = FreemarkerUtils.templateModelForObject(renderableObject);
		return FreemarkerUtils.processTemplate(template, templateModel).getBytes();
	}

}
