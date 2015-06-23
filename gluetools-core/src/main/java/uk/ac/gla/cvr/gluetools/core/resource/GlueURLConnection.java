package uk.ac.gla.cvr.gluetools.core.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class GlueURLConnection extends URLConnection {

	protected GlueURLConnection(URL url) {
		super(url);
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public InputStream getInputStream() throws IOException {
		String file = getURL().getFile();
		byte[] contents = GlueResourceMap.getInstance().get(file);
		if(contents == null) { return null; }
		return new ByteArrayInputStream(contents);
	}

	

}
