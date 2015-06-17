package uk.ac.gla.cvr.gluetools.core;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
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
	private ServerRuntime cayenneServerRuntime;
	
	private GluetoolsEngine() {
		freemarkerConfiguration = new Configuration();
		cayenneServerRuntime = new ServerRuntime("cayenne-gluecore-domain.xml");
	}
	
	public PluginConfigContext createPluginConfigContext() {
		return new PluginConfigContext(freemarkerConfiguration);
	}

	public ObjectContext getCayenneObjectContext() {
		return cayenneServerRuntime.getContext();
	}

}
