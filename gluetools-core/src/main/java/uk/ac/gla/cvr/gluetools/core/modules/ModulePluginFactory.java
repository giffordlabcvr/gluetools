package uk.ac.gla.cvr.gluetools.core.modules;

import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.FastaImporterPlugin;
import uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi.NcbiImporterPlugin;
import uk.ac.gla.cvr.gluetools.core.collation.populating.genbank.GenbankXmlPopulatorPlugin;
import uk.ac.gla.cvr.gluetools.core.collation.populating.textfile.TextFilePopulatorPlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.reporting.RandomMutationsPlugin;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ModulePluginFactory extends PluginFactory<ModulePlugin<?>>{

	public static Multiton.Creator<ModulePluginFactory> creator = new
			Multiton.SuppliedCreator<>(ModulePluginFactory.class, ModulePluginFactory::new);
	
	private ModulePluginFactory() {
		super();
		registerPluginClass(NcbiImporterPlugin.class);
		registerPluginClass(FastaImporterPlugin.class);
		registerPluginClass(GenbankXmlPopulatorPlugin.class);
		registerPluginClass(TextFilePopulatorPlugin.class);
		registerPluginClass(RandomMutationsPlugin.class);
	}
	
}
