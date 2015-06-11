package uk.ac.gla.cvr.gluetools.core.plugins;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
		Field elemNameField = null;
		try {
			elemNameField = theClass.getDeclaredField("ELEM_NAME");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if(elemNameField == null) {
			throw new RuntimeException("Field ELEM_NAME not defined.");
		}
		if(!elemNameField.getType().equals(String.class)) {
			throw new RuntimeException("Field ELEM_NAME not of type String.");
		}
		if( (elemNameField.getModifiers() | Modifier.STATIC) == 0) {
			throw new RuntimeException("Field ELEM_NAME not static");
		}
		if( (elemNameField.getModifiers() | Modifier.PUBLIC) == 0) {
			throw new RuntimeException("Field ELEM_NAME not public");
		}
		String elemName = null;
		try {
			elemName = (String) elemNameField.get(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if(elemName == null) {
			throw new RuntimeException("Field ELEM_NAME not initialized");
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
	
	public P createFromElement(Element element)  {
		String elementName = element.getNodeName();
		Class<? extends P> pluginClass = typeStringToPluginClass.get(elementName);
		if(pluginClass == null) {
			throw new PluginFactoryException(Code.UNKNOWN_ELEMENT_NAME, thisFactoryName, elementName);
		}
		try {
			return createPlugin(pluginClass, element);
		} catch(Exception e) {
			throw new PluginFactoryException(e, Code.PLUGIN_CREATION_FAILED, pluginClass.getCanonicalName());
		}
	}
	
	public List<P> createFromElements(List<Element> elements)  {
		return elements.stream().map(element -> createFromElement(element)).collect(Collectors.toList());
	}

	public static <Q extends Plugin> List<Q> createPlugins(Class<Q> pluginClass, List<Element> elements) {
		return elements.stream().map(e -> createPlugin(pluginClass, e)).collect(Collectors.toList());
	}
	
	public static <Q extends Plugin> Q createPlugin(Class<Q> pluginClass, Element element) {
		Q plugin = null;
		try {
			plugin = pluginClass.newInstance();
		} catch(Exception e) {
			throw new PluginFactoryException(e, Code.PLUGIN_CREATION_FAILED, pluginClass.getCanonicalName());
		}
		PluginUtils.setValidConfigLocal(element);
		plugin.configure(element);
		PluginUtils.checkValidConfig(element);
		return plugin;
	}
	
}
