package uk.ac.gla.cvr.gluetools.core.resource;

import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.ResourceLocator;

public class GlueResourceLocator implements ResourceLocator {

	private ClassLoaderResourceLocator classLoaderResourceLocator = new ClassLoaderResourceLocator();
	
	@Override
	public Collection<Resource> findResources(String name) {
		if(GlueResourceMap.getInstance().get("/"+name) != null) {
			return Collections.singletonList(new GlueResource(name));
		} else {
			return classLoaderResourceLocator.findResources(name);
		}
	}

}
