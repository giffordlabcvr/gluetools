package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.io.IOException;
import java.io.StringWriter;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

public class AbstractFastaExporter<T extends AbstractFastaExporter<T>> extends ModulePlugin<T> {

	private Template idTemplate;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		idTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, "idTemplate", false);
	}

	protected String generateFastaId(Sequence seq) {
		String fastaId;
		if(idTemplate == null) {
			fastaId = seq.getSequenceID();
		} else {
			TemplateHashModel variableResolver = new TemplateHashModel() {
				@Override
				public TemplateModel get(String key) {
					Object propValue = seq.readNestedProperty(key);
					return propValue == null ? null : new SimpleScalar(propValue.toString()); 
				}
				@Override
				public boolean isEmpty() { return false; }
			};
			StringWriter result = new StringWriter();
			try {
				idTemplate.process(variableResolver, result);
			} catch (TemplateException e) {
				throw new CommandException(e, Code.COMMAND_FAILED_ERROR, e.getLocalizedMessage());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			fastaId = result.toString();
		}
		return fastaId;
	}

}
