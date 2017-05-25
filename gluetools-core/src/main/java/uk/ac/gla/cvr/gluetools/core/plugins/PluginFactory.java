package uk.ac.gla.cvr.gluetools.core.plugins;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
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
	
	private Map<String, PluginTypeInfo> elemNameToPluginClassInfo = 
			new LinkedHashMap<String, PluginTypeInfo>();
		
	protected void registerPluginClass(Class<? extends P> theClass) {
		PluginClass pluginClassAnnotation = theClass.getAnnotation(PluginClass.class);
		if(pluginClassAnnotation == null) {
			throw new RuntimeException("No PluginClass annotation on "+theClass.getCanonicalName());
		}
		String elemName = pluginClassAnnotation.elemName();
		if(elemName == null) {
			throw new RuntimeException("No elemName defined on PluginClass annotation on "+theClass.getCanonicalName());
		}
		elemNameToPluginClassInfo.put(elemName, new PluginTypeInfo(theClass, pluginClassAnnotation.deprecated(), pluginClassAnnotation.deprecationWarning()));
	}

	protected void registerPluginClass(String elemName, Class<? extends P> theClass) {
		elemNameToPluginClassInfo.put(elemName, new PluginTypeInfo(theClass));
	}
	
	protected PluginFactory() {
		super();
		this.thisFactoryName = this.getClass().getSimpleName();
	}

	public Set<String> getElementNames() {
		return elemNameToPluginClassInfo.keySet();
	}
	
	public P createFromElement(PluginConfigContext pluginConfigContext, Element element)  {
		P plugin = instantiateFromElement(element);
		configurePlugin(pluginConfigContext, element, plugin);
		return plugin;
	}

	public P instantiateFromElement(Element element) {
		String elementName = element.getNodeName();
		PluginTypeInfo pluginTypeInfo = elemNameToPluginClassInfo.get(elementName);
		Class<? extends P> pluginClass = pluginTypeInfo.getTheClass();
		if(pluginClass == null) {
			throw new PluginFactoryException(Code.UNKNOWN_ELEMENT_NAME, thisFactoryName, elementName);
		}
		P plugin = instantiatePlugin(element, pluginClass);
		if(pluginTypeInfo.isDeprecated()) {
			String deprecationWarning = pluginTypeInfo.getDeprecationWarning();
			if(deprecationWarning.equals(PluginClass.NULL)) {
				deprecationWarning = "Plugin element \""+elementName+"\" is deprecated";
			}
			GlueLogger.getGlueLogger().warning(deprecationWarning);
		}
		return plugin;
	}

	private P instantiatePlugin(Element element, Class<? extends P> pluginClass) {
		P plugin = instantiatePluginStatic(pluginClass, element);
		return plugin;
	}

	public Class<? extends P> classForElementName(String elementName) {
		PluginFactory<P>.PluginTypeInfo pluginTypeInfo = elemNameToPluginClassInfo.get(elementName);
		if(pluginTypeInfo != null) {
			Class<? extends P> pluginClass = pluginTypeInfo.getTheClass();
			return pluginClass;
		}
		return null;
	}

	public boolean containsElementName(String elementName) {
		return elemNameToPluginClassInfo.containsKey(elementName);
	}

	public List<P> createFromElements(PluginConfigContext pluginConfigContext, List<Element> elements)  {
		return elements.stream().map(element -> createFromElement(pluginConfigContext, element)).collect(Collectors.toList());
	}

	public static <Q extends Plugin> List<Q> createPlugins(PluginConfigContext pluginConfigContext, Class<Q> pluginClass, List<Element> elements) {
		return elements.stream().map(e -> {
			Q plugin = instantiatePluginStatic(pluginClass, e);
			configurePlugin(pluginConfigContext, e, plugin);
			return plugin;
		}).collect(Collectors.toList());
	}
	
	public static <Q extends Plugin> Q createPlugin(PluginConfigContext pluginConfigContext, Class<Q> pluginClass, Element element) {
		Q plugin = instantiatePluginStatic(pluginClass, element);
		configurePlugin(pluginConfigContext, element, plugin);
		return plugin;
	}

	private static <Q extends Plugin> Q instantiatePluginStatic(Class<Q> pluginClass,
			Element element) {
		Q plugin = null;
		try {
			plugin = pluginClass.newInstance();
		} catch(Exception e) {
			throw new PluginFactoryException(e, Code.PLUGIN_CREATION_FAILED, pluginClass.getCanonicalName(), e.getMessage());
		}
		PluginUtils.setValidConfigLocal(element);
		return plugin;
	}

	public static <Q extends Plugin> void configurePlugin(
			PluginConfigContext pluginConfigContext, Element element, Q plugin) {
		plugin.configure(pluginConfigContext, element);
		PluginUtils.checkValidConfig(element);
	}
	
	public List<Class<? extends P>> getRegisteredClasses() {
		return new ArrayList<Class<? extends P>>(elemNameToPluginClassInfo.values()
					.stream()
					.map(pci -> pci.getTheClass())
					.collect(Collectors.toList()));
	}
	
	private class PluginTypeInfo {
		private Class<? extends P> theClass;
		private boolean deprecated;
		private String deprecationWarning;
		
		private PluginTypeInfo(Class<? extends P> theClass) {
			this(theClass, false, null);
		}
		
		private PluginTypeInfo(Class<? extends P> theClass, boolean deprecated, String deprecationWarning) {
			super();
			this.theClass = theClass;
			this.deprecated = deprecated;
			this.deprecationWarning = deprecationWarning;
		}

		public String getDeprecationWarning() {
			return deprecationWarning;
		}

		public Class<? extends P> getTheClass() {
			return theClass;
		}

		public boolean isDeprecated() {
			return deprecated;
		}
	}
	
}
