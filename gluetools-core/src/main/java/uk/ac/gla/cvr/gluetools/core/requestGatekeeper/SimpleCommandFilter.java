package uk.ac.gla.cvr.gluetools.core.requestGatekeeper;


import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.requestGatekeeper.RequestFilterException.Code;
import uk.ac.gla.cvr.gluetools.core.requestQueue.Request;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="simpleCommandFilter")
public class SimpleCommandFilter extends BaseRequestFilter {

	private String[] commandWords;
	private Map<String, String> argValues = new LinkedHashMap<String, String>();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		String commandWordsString = PluginUtils.configureString(configElem, "@words", true);
		if(!commandWordsString.matches("^[a-z\\-]+(?: [a-z\\-]+)*$")) {
			throw new RequestFilterException(Code.CONFIG_ERROR, "The words attribute should be a sequence of one or more lower case words "+
		"(possibly with hyphens) separated by single spaces.");
		}
		this.commandWords = commandWordsString.split(" ");
		List<Element> argElems = GlueXmlUtils.findChildElements(configElem, "arg");
		for(Element argElem: argElems) {
			PluginUtils.setValidConfigLocal(argElem);
			Attr nameAttrNode = argElem.getAttributeNode("name");
			if(nameAttrNode == null) {
				throw new RequestFilterException(Code.CONFIG_ERROR, "Missing arg name attribute");
			}
			PluginUtils.setValidConfigLocal(nameAttrNode);
			String name = nameAttrNode.getTextContent();
			String value = argElem.getTextContent();
			if(argValues.containsKey(name)) {
				throw new RequestFilterException(Code.CONFIG_ERROR, "Duplicate arg element with name '"+name+"'");
			}
			argValues.put(name, value);
		}
	}

	@Override
	protected boolean allowRequestLocal(Request request) {
		String[] requestCommandWords = request.getCommandWords();
		if(!Arrays.equals(requestCommandWords, commandWords)) {
			return false;
		}
		Document ownerDocument = request.getCommand().getCmdElem().getOwnerDocument();
		Node parentNode = ownerDocument;
		for(String cmdWord: commandWords) {
			List<Element> childElementsWithName = GlueXmlUtils.findChildElements(parentNode, cmdWord);
			if(childElementsWithName.size() != 1) {
				return false; // shouldn't happen afaik.
			}
			parentNode = childElementsWithName.get(0);
		}
		for(Map.Entry<String, String> entry: argValues.entrySet()) {
			List<Element> childElementsWithName = GlueXmlUtils.findChildElements(parentNode, entry.getKey());
			if(childElementsWithName.size() != 1) {
				return false; // shouldn't happen afaik.
			}
			Element elem = childElementsWithName.get(0);
			if(!elem.getTextContent().equals(entry.getValue())) {
				return false;
			}
		}
		return true;
	}

}

