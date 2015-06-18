package uk.ac.gla.cvr.gluetools.core.collation.importing;

import uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi.NcbiImporterPlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ImporterPluginFactory extends PluginFactory<ImporterPlugin>{

	public static Multiton.Creator<ImporterPluginFactory> creator = new
			Multiton.SuppliedCreator<>(ImporterPluginFactory.class, ImporterPluginFactory::new);
	
	private ImporterPluginFactory() {
		super();
		registerPluginClass(NcbiImporterPlugin.class);
	}
	
}
