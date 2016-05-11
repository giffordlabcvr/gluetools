package uk.ac.gla.cvr.gluetools.core.modules;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.Kuiken2006CodonLabeler;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.FastaExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein.FastaProteinAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.freemarker.FreemarkerTextToGlueTransformer;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.FastaImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.BlastFastaAlignmentImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.BlastFastaProteinAlignmentImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.FastaAlignmentImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi.NcbiImporter;
import uk.ac.gla.cvr.gluetools.core.collation.populating.genbank.GenbankXmlPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.textfile.TextFilePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.referenceBuilder.GbRefBuilder;
import uk.ac.gla.cvr.gluetools.core.commonAaPolymorphisms.CommonAaPolymorphismGenerator;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.codonAwareBlast.CodonAwareBlastAligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.compound.CompoundAligner;
import uk.ac.gla.cvr.gluetools.core.digs.importer.DigsImporter;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.reporting.MutationFrequenciesReporter;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceReporter;
import uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.freemarker.FreemarkerObjectRenderer;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter;
import uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool.WebAnalysisTool;
import uk.ac.gla.cvr.gluetools.core.textToQuery.TextToQueryTransformer;
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
		registerPluginClass(FreemarkerTextToGlueTransformer.class);
		registerPluginClass(MutationFrequenciesReporter.class);
		registerPluginClass(BlastAligner.class);
		registerPluginClass(CodonAwareBlastAligner.class);
		registerPluginClass(CompoundAligner.class);
		
		registerPluginClass(FastaAlignmentImporter.class);
		registerPluginClass(BlastFastaAlignmentImporter.class);
		registerPluginClass(BlastFastaProteinAlignmentImporter.class);
		registerPluginClass(FastaAlignmentExporter.class);
		registerPluginClass(FastaProteinAlignmentExporter.class);
		registerPluginClass(DigsImporter.class);
		registerPluginClass(GbRefBuilder.class);
		registerPluginClass(SamReporter.class);
		registerPluginClass(FastaSequenceReporter.class);
		registerPluginClass(Kuiken2006CodonLabeler.class);
		registerPluginClass(CommonAaPolymorphismGenerator.class);
		registerPluginClass(TextToQueryTransformer.class);
		registerPluginClass(FreemarkerObjectRenderer.class);

		registerPluginClass(WebAnalysisTool.class);
		
	}
	
}
