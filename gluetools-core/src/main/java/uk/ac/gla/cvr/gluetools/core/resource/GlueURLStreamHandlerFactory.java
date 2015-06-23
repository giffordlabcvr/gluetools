package uk.ac.gla.cvr.gluetools.core.resource;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class GlueURLStreamHandlerFactory implements URLStreamHandlerFactory {

	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if ("glue".equals(protocol)) {
            return new GlueURLStreamHandler();
        }
        return null;
    }

}
