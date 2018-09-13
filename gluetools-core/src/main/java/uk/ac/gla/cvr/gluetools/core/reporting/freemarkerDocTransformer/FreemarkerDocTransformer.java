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

import java.io.Writer;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

@PluginClass(elemName="freemarkerDocTransformer",
		description="Transforms an input GLUE command document into an output byte array (e.g. html)")
public class FreemarkerDocTransformer extends ModulePlugin<FreemarkerDocTransformer> {

	public static String TEMPLATE_FILE_NAME = "templateFileName";
	// can be used e.g. for inline images.
	public static String RESOURCE_FILE_NAME = "resourceFileName";
	
	private String templateFileName;
	private List<String> resourceFileNames;

	private Template template = null;
	
	public FreemarkerDocTransformer() {
		super();
		addSimplePropertyName(TEMPLATE_FILE_NAME);
		registerModulePluginCmdClass(TransformToFileCommand.class);
		registerModulePluginCmdClass(TransformToWebFileCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.templateFileName = PluginUtils.configureStringProperty(configElem, TEMPLATE_FILE_NAME, true);
		registerResourceName(templateFileName);
		this.resourceFileNames = PluginUtils.configureStringsProperty(configElem, RESOURCE_FILE_NAME);
		for(String resourceFileName: this.resourceFileNames) {
			registerResourceName(resourceFileName);
		}
	}
	
	public byte[] renderToBytes(CommandContext cmdContext, CommandDocument commandDocument) {
		Map<String, Object> rootModel = initRootModel(cmdContext, commandDocument);
		return FreemarkerUtils.processTemplate(template, rootModel).getBytes();
	}

	public void renderToWriter(CommandContext cmdContext, CommandDocument commandDocument, Writer writer) {
		Map<String, Object> rootModel = initRootModel(cmdContext, commandDocument);
		FreemarkerUtils.processTemplate(writer, template, rootModel);
	}

	private Map<String, Object> initRootModel(CommandContext cmdContext,
			CommandDocument commandDocument) {
		if(template == null) {
			byte[] templateBytes = getResource(cmdContext, templateFileName);
			Configuration freemarkerConfiguration = cmdContext.getGluetoolsEngine().getFreemarkerConfiguration();
			template = FreemarkerUtils.templateFromString(
					// xml extension triggers xml escaping
					getModuleName()+":"+templateFileName, new String(templateBytes),  
						freemarkerConfiguration);
		}
		TemplateModel cmdDocModel = FreemarkerUtils.templateModelForObject(commandDocument);
		Map<String, Object> rootModel = new LinkedHashMap<String, Object>();
		rootModel.put("getResourceAsBase64", new GetResourceAsBase64Method(cmdContext));
		rootModel.put("getResourceAsString", new GetResourceAsStringMethod(cmdContext));
		rootModel.put(commandDocument.getRootName(), cmdDocModel);
		return rootModel;
	}

	public abstract class GetResourceMethod implements TemplateMethodModelEx {
		private CommandContext cmdContext;

	    public GetResourceMethod(CommandContext cmdContext) {
			super();
			this.cmdContext = cmdContext;
		}
	    
		@SuppressWarnings("rawtypes")
		public final Object exec(List args) throws TemplateModelException {
	        if (args.size() != 1) {
	            throw new TemplateModelException("Wrong number of arguments");
	        }
	        String resourceFileName = args.get(0).toString();
			if(!resourceFileNames.contains(resourceFileName)) {
				throw new TemplateModelException(
	                    "Unknown resource file \""+resourceFileName);
			}
	    	byte[] resourceFileBytes = getResource(cmdContext, resourceFileName);
			return objectFromResourceFileBytes(resourceFileBytes);
	    }

		protected abstract Object objectFromResourceFileBytes(byte[] resourceFileBytes);
		

	}
	public class GetResourceAsBase64Method extends GetResourceMethod{
	    public GetResourceAsBase64Method(CommandContext cmdContext) {
			super(cmdContext);
		}
	    @Override
	    protected Object objectFromResourceFileBytes(byte[] resourceFileBytes) {
	    	return new String(Base64.getEncoder().encode(resourceFileBytes));
	    }
	}
	public class GetResourceAsStringMethod extends GetResourceMethod{
	    public GetResourceAsStringMethod(CommandContext cmdContext) {
			super(cmdContext);
		}
	    @Override
	    protected Object objectFromResourceFileBytes(byte[] resourceFileBytes) {
	    	return new String(resourceFileBytes);
	    }
	}

	
}
