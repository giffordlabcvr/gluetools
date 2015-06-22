package uk.ac.gla.cvr.gluetools.core.plugins;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException.Code;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

// TODO would be nice to register plugin classes using an annotation processor

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
		
	protected void registerPluginClass(Class<? extends P> theClass) {
		PluginClass pluginClassAnnotation = theClass.getAnnotation(PluginClass.class);
		if(pluginClassAnnotation == null) {
			throw new RuntimeException("No PluginClass annotation on "+theClass.getCanonicalName());
		}
		String elemName = pluginClassAnnotation.elemName();
		if(elemName == null) {
			throw new RuntimeException("No elemName defined on PluginClass annotation on "+theClass.getCanonicalName());
		}
		typeStringToPluginClass.put(elemName, theClass);
	}
	
	protected PluginFactory() {
		super();
		this.thisFactoryName = this.getClass().getSimpleName();
	}

	public Set<String> getElementNames() {
		return typeStringToPluginClass.keySet();
	}
	
	public P createFromElement(PluginConfigContext pluginConfigContext, Element element)  {
		String elementName = element.getNodeName();
		Class<? extends P> pluginClass = classForElementName(elementName);
		if(pluginClass == null) {
			throw new PluginFactoryException(Code.UNKNOWN_ELEMENT_NAME, thisFactoryName, elementName);
		}
		P plugin = instantiatePlugin(element, pluginClass);
		configurePluginStatic(pluginConfigContext, element, plugin);
		return plugin;
	}

	protected P instantiatePlugin(Element element, Class<? extends P> pluginClass) {
		P plugin = instantiatePluginStatic(pluginClass, element);
		return plugin;
	}

	public Class<? extends P> classForElementName(String elementName) {
		Class<? extends P> pluginClass = typeStringToPluginClass.get(elementName);
		return pluginClass;
	}
	
	public List<P> createFromElements(PluginConfigContext pluginConfigContext, List<Element> elements)  {
		return elements.stream().map(element -> createFromElement(pluginConfigContext, element)).collect(Collectors.toList());
	}

	public static <Q extends Plugin> List<Q> createPlugins(PluginConfigContext pluginConfigContext, Class<Q> pluginClass, List<Element> elements) {
		return elements.stream().map(e -> {
			Q plugin = instantiatePluginStatic(pluginClass, e);
			configurePluginStatic(pluginConfigContext, e, plugin);
			return plugin;
		}).collect(Collectors.toList());
	}
	
	public static <Q extends Plugin> Q createPlugin(PluginConfigContext pluginConfigContext, Class<Q> pluginClass, Element element) {
		Q plugin = instantiatePluginStatic(pluginClass, element);
		configurePluginStatic(pluginConfigContext, element, plugin);
		return plugin;
	}

	private static <Q extends Plugin> Q instantiatePluginStatic(Class<Q> pluginClass,
			Element element) {
		Q plugin = null;
		try {
			plugin = pluginClass.newInstance();
		} catch(Exception e) {
			throw new PluginFactoryException(e, Code.PLUGIN_CREATION_FAILED, pluginClass.getCanonicalName());
		}
		PluginUtils.setValidConfigLocal(element);
		return plugin;
	}

	private static <Q extends Plugin> void configurePluginStatic(
			PluginConfigContext pluginConfigContext, Element element, Q plugin) {
		plugin.configure(pluginConfigContext, element);
		PluginUtils.checkValidConfig(element);
	}
	
	public List<Class<? extends P>> getRegisteredClasses() {
		return new ArrayList<Class<? extends P>>(typeStringToPluginClass.values());
	}
	
}
