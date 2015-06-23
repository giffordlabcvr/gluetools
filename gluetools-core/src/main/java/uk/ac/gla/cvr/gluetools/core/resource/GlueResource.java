package uk.ac.gla.cvr.gluetools.core.resource;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cayenne.resource.Resource;

public class GlueResource implements Resource {

	private String fileName;
	
	public GlueResource(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public URL getURL() {
		try {
			return new URL("glue", "host", "/"+fileName);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Resource getRelativeResource(String relativePath) {
		File relativeFile = new File(relativePath);
		return new GlueResource(relativeFile.getName());
	}

}
