package uk.ac.gla.cvr.gluetools.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.UUID;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtilsException.Code;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

public class FreemarkerUtils {

	
	public static Template templateFromString(String templateString, Configuration freemarkerConfiguration) {
		try {
			Template template = new Template(UUID.randomUUID().toString(), new StringReader(templateString), freemarkerConfiguration);
			return template;
		} catch (ParseException pe) {
			throw new FreemarkerUtilsException(pe, Code.INVALID_FREEMARKER_TEMPLATE, templateString, pe.getLocalizedMessage());
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} 
	}
	
	public static class GlueDataObjectTemplateModel implements TemplateHashModel {

		private GlueDataObject glueDataObject;
		
		public GlueDataObjectTemplateModel(GlueDataObject glueDataObject) {
			super();
			this.glueDataObject = glueDataObject;
		}
		@Override
		public TemplateModel get(String key) {
			Object propValue = glueDataObject.readProperty(key);
			if(propValue == null) {
				return null;
			}
			if(propValue instanceof GlueDataObject) {
				return new GlueDataObjectTemplateModel((GlueDataObject) propValue);
			}
			return new SimpleScalar(propValue.toString()); 
		}
		@Override
		public boolean isEmpty() { return false; }

	}

	public static String processTemplate(Template template, TemplateModel templateModel) {
		StringWriter result = new StringWriter();
		try {
			template.process(templateModel, result);
		} catch (TemplateException e) {
			throw new FreemarkerUtilsException(e, Code.FREEMARKER_TEMPLATE_FAILED, e.getLocalizedMessage());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result.toString();
	}

	
}
