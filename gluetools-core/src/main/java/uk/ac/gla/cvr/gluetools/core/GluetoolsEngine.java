package uk.ac.gla.cvr.gluetools.core;

import java.net.URL;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.resource.GlueURLStreamHandlerFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;
import freemarker.template.Configuration;

public class GluetoolsEngine {

	private static Multiton instances = new Multiton();
	
	private static Multiton.Creator<GluetoolsEngine> creator = new
			Multiton.SuppliedCreator<>(GluetoolsEngine.class, GluetoolsEngine::new);
	
	public static GluetoolsEngine getInstance() {
		return instances.get(creator);
	}
	
	private Configuration freemarkerConfiguration;
	
	private GluetoolsEngine() {
		freemarkerConfiguration = new Configuration();
		URL.setURLStreamHandlerFactory(new GlueURLStreamHandlerFactory());
	}
	
	public PluginConfigContext createPluginConfigContext() {
		return new PluginConfigContext(freemarkerConfiguration);
	}


}
