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
package uk.ac.gla.cvr.gluetools.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.UUID;

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
	// renderableObject can definitely be a GlueDataObject or a Collection
	public static TemplateModel templateModelForObject(Object renderableObject) {
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
