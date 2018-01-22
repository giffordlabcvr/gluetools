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
package uk.ac.gla.cvr.gluetools.core.reporting.figtree.annotationExporter;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class FigTreeAnnotation implements Plugin {

	public static final String ANNOTATION_NAME = "annotationName";
	public static final String VALUE_FREEMARKER_TEMPLATE = "valueFreemarkerTemplate";
	
	private String annotationName;
	private Template valueFreemarkerTemplate;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		this.annotationName = PluginUtils.configureStringProperty(configElem, ANNOTATION_NAME, true);
		this.valueFreemarkerTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, VALUE_FREEMARKER_TEMPLATE, false);
	}

	public String getAnnotationName() {
		return annotationName;
	}

	public void setAnnotationName(String annotationName) {
		this.annotationName = annotationName;
	}

	public Template getValueFreemarkerTemplate(Configuration freemarkerConfiguration) {
		if(this.valueFreemarkerTemplate == null) {
			return FreemarkerUtils.templateFromString("${renderNestedProperty('"+this.annotationName+"')}", 
					freemarkerConfiguration);
		}
		return valueFreemarkerTemplate;
	}
	
	
}
