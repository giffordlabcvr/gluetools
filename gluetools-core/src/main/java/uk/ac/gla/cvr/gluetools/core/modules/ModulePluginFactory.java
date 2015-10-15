package uk.ac.gla.cvr.gluetools.core.modules;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.FastaExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.FastaImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.FastaAlignmentImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi.NcbiImporter;
import uk.ac.gla.cvr.gluetools.core.collation.populating.genbank.GenbankXmlPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.textfile.TextFilePopulator;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAligner;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.reporting.MutationFrequenciesReporter;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ModulePluginFactory extends PluginFactory<ModulePlugin<?>>{

	public static Multiton.Creator<ModulePluginFactory> creator = new
			Multiton.SuppliedCreator<>(ModulePluginFactory.class, ModulePluginFactory::new);
	
	private ModulePluginFactory() {
		super();
		registerPluginClass(NcbiImporter.class);
		registerPluginClass(FastaImporter.class);
		registerPluginClass(FastaExporter.class);
		registerPluginClass(GenbankXmlPopulator.class);
		registerPluginClass(TextFilePopulator.class);
		registerPluginClass(MutationFrequenciesReporter.class);
		registerPluginClass(BlastAligner.class);
		registerPluginClass(FastaAlignmentImporter.class);
		registerPluginClass(FastaAlignmentExporter.class);
	}
	
}
