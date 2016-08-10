package uk.ac.gla.cvr.gluetools.core.classloader;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;

// Delete this class!
public class GlueAdhocObjectFactory extends DefaultAdhocObjectFactory {

    @Inject
    protected Injector injector;

	@Override
	public <T> T newInstance(Class<? super T> superType, String className) {
		super.injector = injector;
		return super.newInstance(superType, className);
	}

	/*
	@Override
	public Class<?> getJavaClass(String className) throws ClassNotFoundException {
		super.injector = injector;
		try {
			return super.getJavaClass(className);
		} catch(ClassNotFoundException cnfe) {
			return GluetoolsEngine.getInstance().getGlueClassLoader().loadClass(className);
		}
	}
	*/
}
