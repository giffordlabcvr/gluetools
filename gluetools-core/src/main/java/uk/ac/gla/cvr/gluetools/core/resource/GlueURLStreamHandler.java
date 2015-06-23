package uk.ac.gla.cvr.gluetools.core.resource;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class GlueURLStreamHandler extends URLStreamHandler {

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return new GlueURLConnection(url);
	}

}
