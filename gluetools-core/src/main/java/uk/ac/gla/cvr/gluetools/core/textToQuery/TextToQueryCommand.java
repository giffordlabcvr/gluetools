package uk.ac.gla.cvr.gluetools.core.textToQuery;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"text-to-query"}, 
		description = "Transform text to an object query", 
		docoptUsages = { "<text>" }, 
		metaTags = {}	
)
public class TextToQueryCommand extends ModulePluginCommand<ListResult, TextToQueryTransformer> {

	public static final String TEXT = "text";

	private String text;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.text = PluginUtils.configureStringProperty(configElem, TEXT, true);
	}

	@Override
	protected ListResult execute(CommandContext cmdContext, TextToQueryTransformer textToQueryTransformer) {
		return textToQueryTransformer.textToQuery(cmdContext, text);
	}

}
