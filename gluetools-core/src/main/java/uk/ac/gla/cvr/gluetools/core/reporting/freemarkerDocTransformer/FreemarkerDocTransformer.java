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
package uk.ac.gla.cvr.gluetools.core.reporting.freemarkerDocTransformer;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

@PluginClass(elemName="freemarkerDocTransformer",
		description="Transforms an input GLUE command document into an output byte array (e.g. html)")
public class FreemarkerDocTransformer extends ModulePlugin<FreemarkerDocTransformer> {

	public static String TEMPLATE_FILE_NAME = "templateFileName";
	
	private String templateFileName;

	private Template template = null;
	
	public FreemarkerDocTransformer() {
		super();
		addSimplePropertyName(TEMPLATE_FILE_NAME);
		registerModulePluginCmdClass(TransformToFileCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.templateFileName = PluginUtils.configureStringProperty(configElem, TEMPLATE_FILE_NAME, true);
		registerResourceName(templateFileName);
	}
	
	public byte[] renderToBytes(CommandContext cmdContext, CommandDocument commandDocument) {
		if(template == null) {
			byte[] templateBytes = getResource(cmdContext, templateFileName);
			template = FreemarkerUtils.templateFromString(
					// xml extension triggers xml escaping
					getModuleName()+":"+templateFileName, new String(templateBytes),  
						cmdContext.getGluetoolsEngine().getFreemarkerConfiguration());
		}
		TemplateModel templateModel = FreemarkerUtils.templateModelForObject(commandDocument);
		return FreemarkerUtils.processTemplate(template, templateModel).getBytes();
	}

}
