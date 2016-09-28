package uk.ac.gla.cvr.gluetools.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.UUID;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtilsException.Code;
import freemarker.core.Environment;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class FreemarkerUtils {

	
	public static Template templateFromString(String templateString, Configuration freemarkerConfiguration) {
		return templateFromString(UUID.randomUUID().toString(), templateString, freemarkerConfiguration);
	}

	public static Template templateFromString(String templateName, String templateString, Configuration freemarkerConfiguration) {
		try {
			Template template = new Template(templateName, new StringReader(templateString), freemarkerConfiguration);
			return template;
		} catch (ParseException pe) {
			throw new FreemarkerUtilsException(pe, Code.INVALID_FREEMARKER_TEMPLATE, templateString, pe.getLocalizedMessage());
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} 
	}

	
	public static String processTemplate(Template template, TemplateModel templateModel) {
		StringWriter result = new StringWriter();
		try {
			Environment env = template.createProcessingEnvironment(templateModel, result);
			
			env.process();
		} catch (TemplateException e) {
			throw new FreemarkerUtilsException(e, Code.FREEMARKER_TEMPLATE_FAILED, e.getLocalizedMessage());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result.toString();
	}

	// pretty sure this can be replaced by Configuration.getObjectWrapper().wrap(renderableObject)?
	public static TemplateModel templateModelForGlueDataObject(GlueDataObject renderableObject) {
		DefaultObjectWrapperBuilder objectWrapperBuilder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_21);
		DefaultObjectWrapper objectWrapper = objectWrapperBuilder.build();

		TemplateModel templateModel = null;
		try {
			templateModel = objectWrapper.wrap(renderableObject);
		} catch (TemplateModelException e) {
			throw new FreemarkerUtilsException(e, Code.FREEMARKER_TEMPLATE_FAILED, e.getLocalizedMessage());
		}
		return templateModel;
	}
	
}
