package uk.ac.gla.cvr.gluetools.core.modules;

import uk.ac.gla.cvr.gluetools.core.blastRecogniser.BlastSequenceRecogniser;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.Kuiken2006CodonLabeler;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.FastaExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein.FastaProteinAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus.NucleotideConsensusGenerator;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus.protein.AminoAcidConsensusGenerator;
import uk.ac.gla.cvr.gluetools.core.collation.freemarker.FreemarkerTextToGlueTransformer;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.FastaImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.BlastFastaAlignmentImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.BlastFastaProteinAlignmentImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.FastaAlignmentImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi.NcbiImporter;
import uk.ac.gla.cvr.gluetools.core.collation.populating.genbank.GenbankXmlPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.textfile.TextFilePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.referenceBuilder.GbRefBuilder;
import uk.ac.gla.cvr.gluetools.core.commonAaAnalyser.CommonAaAnalyser;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.MafftAligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.codonAwareBlast.CodonAwareBlastAligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.compound.CompoundAligner;
import uk.ac.gla.cvr.gluetools.core.curation.phylogeny.raxml.RaxmlPhylogenyGenerator;
import uk.ac.gla.cvr.gluetools.core.curation.sequenceMergers.AlignmentBasedSequenceMerger;
import uk.ac.gla.cvr.gluetools.core.digs.importer.DigsImporter;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvoker;
import uk.ac.gla.cvr.gluetools.core.fastaUtility.FastaUtility;
import uk.ac.gla.cvr.gluetools.core.featurePresenceRecorder.FeaturePresenceRecorder;
import uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood.MaxLikelihoodGenotyper;
import uk.ac.gla.cvr.gluetools.core.modeltest.ModelTester;
import uk.ac.gla.cvr.gluetools.core.phyloUtility.PhyloUtility;
import uk.ac.gla.cvr.gluetools.core.phylogenyImporter.PhyloImporter;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceReporter;
import uk.ac.gla.cvr.gluetools.core.reporting.figtree.annotationExporter.FigTreeAnnotationExporter;
import uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.freemarker.FreemarkerObjectRenderer;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter;
import uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool.WebAnalysisTool;
import uk.ac.gla.cvr.gluetools.core.tabularUtility.TabularUtility;
import uk.ac.gla.cvr.gluetools.core.textToQuery.TextToQueryTransformer;
import uk.ac.gla.cvr.gluetools.core.treerenderer.PhyloExporter;
import uk.ac.gla.cvr.gluetools.core.treetransformer.TreeTransformer;
import uk.ac.gla.cvr.gluetools.core.variationFrequencies.VariationFrequenciesGenerator;
import uk.ac.gla.cvr.gluetools.core.variationscanner.ComparisonAminoAcidVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.ExactMatchAminoAcidVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.ExactMatchNucleotideVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.RegexAminoAcidVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.RegexNucleotideVariationScanner;
import uk.ac.gla.cvr.gluetools.custom.epitopeRasOverlap.EpitopeRasOverlap;
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
		registerPluginClass(BlastAligner.class);
		registerPluginClass(CodonAwareBlastAligner.class);
		registerPluginClass(CompoundAligner.class);
		
		registerPluginClass(FastaAlignmentImporter.class);
		registerPluginClass(BlastFastaAlignmentImporter.class);
		registerPluginClass(BlastFastaProteinAlignmentImporter.class);
		
		registerPluginClass(MafftAligner.class);
		
		registerPluginClass(FastaAlignmentExporter.class);
		registerPluginClass(FastaProteinAlignmentExporter.class);
		registerPluginClass(NucleotideConsensusGenerator.class);
		registerPluginClass(AminoAcidConsensusGenerator.class);
		
		registerPluginClass(DigsImporter.class);
		registerPluginClass(GbRefBuilder.class);
		registerPluginClass(SamReporter.class);
		registerPluginClass(FastaSequenceReporter.class);
		registerPluginClass(Kuiken2006CodonLabeler.class);
		registerPluginClass(TextToQueryTransformer.class);
		registerPluginClass(FreemarkerObjectRenderer.class);
		registerPluginClass(VariationFrequenciesGenerator.class);

		registerPluginClass(FeaturePresenceRecorder.class);
		
		registerPluginClass(FigTreeAnnotationExporter.class);

		registerPluginClass(WebAnalysisTool.class);

		registerPluginClass(PhyloExporter.class);

		registerPluginClass(MaxLikelihoodPlacer.class);
		registerPluginClass(MaxLikelihoodGenotyper.class);
		
		registerPluginClass(TreeTransformer.class);
		
		registerPluginClass(ComparisonAminoAcidVariationScanner.class);
		registerPluginClass(ExactMatchAminoAcidVariationScanner.class);
		registerPluginClass(RegexAminoAcidVariationScanner.class);
		registerPluginClass(ExactMatchNucleotideVariationScanner.class);
		registerPluginClass(RegexNucleotideVariationScanner.class);

		registerPluginClass(RaxmlPhylogenyGenerator.class);
		registerPluginClass(ModelTester.class);

		registerPluginClass(PhyloImporter.class);
		registerPluginClass(PhyloUtility.class);

		registerPluginClass(AlignmentColumnsSelector.class);

		registerPluginClass(AlignmentBasedSequenceMerger.class);
		
		registerPluginClass(CommonAaAnalyser.class);

		registerPluginClass(EcmaFunctionInvoker.class);

		registerPluginClass(TabularUtility.class);
		registerPluginClass(FastaUtility.class);
		
		registerPluginClass(BlastSequenceRecogniser.class);
		
		// custom project modules
		registerPluginClass(EpitopeRasOverlap.class);
		
	}
	
}
