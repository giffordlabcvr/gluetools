package uk.ac.gla.cvr.gluetools.core.plugins;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException.Code;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class PluginFactory<P extends Plugin> {

	private static Multiton factories = new Multiton();
	
	public static <Q extends Plugin, 
		F extends PluginFactory<Q>,
		C extends Multiton.Creator<F>> F get(C creator) {
		return factories.get(creator);
	}
	
	private final String thisFactoryName;
	
	private Map<String, Class<? extends P>> typeStringToPluginClass = 
			new LinkedHashMap<String, Class<? extends P>>();
	
	protected void registerPluginClass(String elemName, Class<? extends P> theClass) {
		typeStringToPluginClass.put(elemName, theClass);
	}
	
	protected PluginFactory() {
		super();
		this.thisFactoryName = this.getClass().getSimpleName();
	}

	public P createFromElement(Element element)  {
		String elementName = element.getNodeName();
		Class<? extends P> pluginClass = typeStringToPluginClass.get(elementName);
		if(pluginClass == null) {
			throw new PluginFactoryException(Code.UNKNOWN_ELEMENT_NAME, thisFactoryName, elementName);
		}
		P plugin = null;
		try {
			plugin = pluginClass.newInstance();
		} catch(Exception e) {
			throw new PluginFactoryException(e, Code.PLUGIN_CREATION_FAILED, thisFactoryName, elementName);
		}
		plugin.configure(element);
		return plugin;
	}
	
	public List<P> createFromElements(List<Element> elements)  {
		return elements.stream().map(element -> createFromElement(element)).collect(Collectors.toList());
	}
	
}
