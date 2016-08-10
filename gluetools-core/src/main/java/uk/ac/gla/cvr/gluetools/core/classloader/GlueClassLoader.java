package uk.ac.gla.cvr.gluetools.core.classloader;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;

public class GlueClassLoader extends ClassLoader {

	private Map<String, Class<?>> classNameToClass = new LinkedHashMap<String, Class<?>>();
	private GluetoolsEngine gluetoolsEngine;
	
	public GlueClassLoader(ClassLoader parent, GluetoolsEngine gluetoolsEngine) {
		super(parent);
		this.gluetoolsEngine = gluetoolsEngine;
	}

	public GlueClassLoader(GluetoolsEngine gluetoolsEngine) {
		super();
		this.gluetoolsEngine = gluetoolsEngine;
	}

	@Override
	protected synchronized Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> theClass = classNameToClass.get(name);
		if(theClass != null) {
			return theClass;
		}
		byte[] bytes = gluetoolsEngine.getBytes(name);
		if(bytes != null) {
			theClass = defineClass(name, bytes, 0, bytes.length);
			classNameToClass.put(name, theClass);
			return theClass;
		}
		// throw classNotFound
		return super.findClass(name);
	}
	
}
