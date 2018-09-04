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
package uk.ac.gla.cvr.gluetools.core.plugins;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	
	private Map<String, PluginClassInfo> elemNameToPluginClassInfo = 
			new LinkedHashMap<String, PluginClassInfo>();
		
	protected void registerPluginClass(Class<? extends P> theClass) {
		PluginClass pluginClassAnnotation = theClass.getAnnotation(PluginClass.class);
		if(pluginClassAnnotation == null) {
			throw new RuntimeException("No PluginClass annotation on "+theClass.getCanonicalName());
		}
		String elemName = pluginClassAnnotation.elemName();
		if(elemName == null) {
			throw new RuntimeException("No elemName defined on PluginClass annotation on "+theClass.getCanonicalName());
		}
		PluginClassInfo pluginClassInfo = new PluginClassInfo(theClass, Optional.of(pluginClassAnnotation));
		registerPluginClassInternal(elemName, pluginClassInfo);
	}

	protected void registerPluginClassInternal(String elemName, PluginClassInfo pluginClassInfo) {
		elemNameToPluginClassInfo.put(elemName, pluginClassInfo);
	}

	protected void registerPluginClass(String elemName, Class<? extends P> theClass) {
		PluginClassInfo pluginClassInfo = new PluginClassInfo(theClass, Optional.empty());
		registerPluginClassInternal(elemName, pluginClassInfo);
	}
	
	protected PluginFactory() {
		super();
		this.thisFactoryName = this.getClass().getSimpleName();
	}

	public Set<String> getElementNames() {
		return elemNameToPluginClassInfo.keySet();
	}
	
	public PluginClassInfo getPluginClassInfo(String elementName) {
		return elemNameToPluginClassInfo.get(elementName);
	}
	
	public P createFromElement(PluginConfigContext pluginConfigContext, Element element)  {
		P plugin = instantiateFromElement(element);
		configurePlugin(pluginConfigContext, element, plugin);
		return plugin;
	}

	public P instantiateFromElement(Element element) {
		String elementName = element.getNodeName();
		PluginClassInfo pluginTypeInfo = elemNameToPluginClassInfo.get(elementName);
		if(pluginTypeInfo == null) {
			throw new PluginFactoryException(Code.UNKNOWN_ELEMENT_NAME, thisFactoryName, elementName);
		}
		Class<? extends P> pluginClass = pluginTypeInfo.getTheClass();
		P plugin = instantiatePlugin(element, pluginClass);
		if(pluginTypeInfo.isDeprecated()) {
			String deprecationWarning = "Plugin element \""+elementName+"\" is deprecated";
			String additionalWarning = pluginTypeInfo.getDeprecationWarning();
			if(!deprecationWarning.equals(PluginClass.NULL)) {
				deprecationWarning = deprecationWarning+": "+additionalWarning;
			}
			GlueLogger.getGlueLogger().warning(deprecationWarning);
		}
		return plugin;
	}

	private P instantiatePlugin(Element element, Class<? extends P> pluginClass) {
		P plugin = instantiatePluginWithElement(pluginClass, element);
		return plugin;
	}

	public Class<? extends P> classForElementName(String elementName) {
		PluginFactory<P>.PluginClassInfo pluginTypeInfo = elemNameToPluginClassInfo.get(elementName);
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
			Q plugin = instantiatePluginWithElement(pluginClass, e);
			configurePlugin(pluginConfigContext, e, plugin);
			return plugin;
		}).collect(Collectors.toList());
	}
	
	public static <Q extends Plugin> Q createPlugin(PluginConfigContext pluginConfigContext, Class<Q> pluginClass, Element element) {
		Q plugin = instantiatePluginWithElement(pluginClass, element);
		configurePlugin(pluginConfigContext, element, plugin);
		return plugin;
	}

	private static <Q extends Plugin> Q instantiatePluginWithElement(Class<Q> pluginClass, Element element) {
		Q plugin = instantiatePlugin(pluginClass);
		PluginUtils.setValidConfigLocal(element);
		return plugin;
	}

	private static <Q extends Plugin> Q instantiatePlugin(
			Class<Q> pluginClass) {
		Q plugin = null;
		try {
			plugin = pluginClass.newInstance();
		} catch(Exception e) {
			throw new PluginFactoryException(e, Code.PLUGIN_CREATION_FAILED, pluginClass.getCanonicalName(), e.getMessage());
		}
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
	
	public class PluginClassInfo {
		private Class<? extends P> theClass;
		private Optional<PluginClass> pluginClassAnnotation;
		private P exampleInstance;
		
		private PluginClassInfo(Class<? extends P> theClass, Optional<PluginClass> pluginClassAnnotation) {
			super();
			this.theClass = theClass;
			this.exampleInstance = instantiatePlugin(theClass);
			this.pluginClassAnnotation = pluginClassAnnotation;
		}

		public String getDeprecationWarning() {
			return pluginClassAnnotation.map(pca -> pca.deprecationWarning()).orElse(null);
		}

		public Class<? extends P> getTheClass() {
			return theClass;
		}

		public P getExampleInstance() {
			return exampleInstance;
		}
		
		public boolean isDeprecated() {
			return pluginClassAnnotation.map(pca -> pca.deprecated()).orElse(false);
		}

		public boolean includeInWebDocs() {
			return pluginClassAnnotation.map(pca -> pca.includeInWebDocs()).orElse(true);
		}

		public String getDescription() {
			return pluginClassAnnotation.map(pca -> pca.description()).orElse(null);
		}

	}
	
}
