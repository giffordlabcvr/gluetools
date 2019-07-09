package uk.ac.gla.cvr.gluetools.core.requestGatekeeper;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.requestGatekeeper.RequestFilterException.Code;
import uk.ac.gla.cvr.gluetools.core.requestQueue.Request;

@PluginClass(elemName="commandWordsFilter")
public class CommandWordsRequestFilter extends BaseRequestFilter {

	private String commandWords;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.commandWords = PluginUtils.configureStringProperty(configElem, "commandWords", true);
		if(!commandWords.matches("^[a-z\\-]+(?: [a-z\\-]+)*$")) {
			throw new RequestFilterException(Code.CONFIG_ERROR, "The commandWords property should be a sequence of one or more lower case words "+
		"(possibly with hyphens) separated by single spaces.");
		}
	}

	@Override
	protected boolean fiterRequestInternal(Request request) {
		List<String> commandWordsList = request.getCommandWords();
		String commandWordsString = String.join(" ", commandWordsList);
		return commandWordsString.equals(commandWords);
	}

}
