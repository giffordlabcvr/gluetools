/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.modules;

import uk.ac.gla.cvr.gluetools.core.blastRecogniser.BlastSequenceRecogniser;
import uk.ac.gla.cvr.gluetools.core.blastRotator.BlastSequenceRotator;
import uk.ac.gla.cvr.gluetools.core.clusterPickerRunner.ClusterPickerRunner;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.Kuiken2006CodonLabeler;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.protein.FastaProteinAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus.NucleotideConsensusGenerator;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.consensus.protein.AminoAcidConsensusGenerator;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.fastaExporter.FastaExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fastaProteinExporter.FastaProteinExporter;
import uk.ac.gla.cvr.gluetools.core.collation.freemarker.FreemarkerTextToGlueTransformer;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.FastaImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.BlastFastaAlignmentImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.BlastFastaProteinAlignmentImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.FastaAlignmentImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi.NcbiImporter;
import uk.ac.gla.cvr.gluetools.core.collation.populating.genbank.GenbankXmlPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.textfile.TextFilePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.referenceBuilder.GbRefBuilder;
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
import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.GbSubmisisonGenerator;
import uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood.MaxLikelihoodGenotyper;
import uk.ac.gla.cvr.gluetools.core.modeltest.ModelTester;
import uk.ac.gla.cvr.gluetools.core.phyloUtility.PhyloUtility;
import uk.ac.gla.cvr.gluetools.core.phylogenyImporter.PhyloImporter;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginGroupRegistry;
import uk.ac.gla.cvr.gluetools.core.replacementClassification.Hanada2006ReplacementClassifier;
import uk.ac.gla.cvr.gluetools.core.replacementDistance.Grantham1974DistanceCalculator;
import uk.ac.gla.cvr.gluetools.core.replacementDistance.Miyata1979DistanceCalculator;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceReporter;
import uk.ac.gla.cvr.gluetools.core.reporting.figtree.annotationExporter.FigTreeAnnotationExporter;
import uk.ac.gla.cvr.gluetools.core.reporting.freemarkerDocTransformer.FreemarkerDocTransformer;
import uk.ac.gla.cvr.gluetools.core.reporting.nexusExporter.NexusExporter;
import uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.freemarker.FreemarkerObjectRenderer;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter;
import uk.ac.gla.cvr.gluetools.core.samFileGenerator.SamFileGenerator;
import uk.ac.gla.cvr.gluetools.core.tabularUtility.TabularUtility;
import uk.ac.gla.cvr.gluetools.core.textToQuery.TextToQueryTransformer;
import uk.ac.gla.cvr.gluetools.core.translationModification.TranslationModifier;
import uk.ac.gla.cvr.gluetools.core.treeVisualiser.TreeVisualiser;
import uk.ac.gla.cvr.gluetools.core.treerenderer.PhyloExporter;
import uk.ac.gla.cvr.gluetools.core.treetransformer.TreeTransformer;
import uk.ac.gla.cvr.gluetools.core.variationFrequencies.VariationFrequenciesGenerator;
import uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.VisualisationUtility;
import uk.ac.gla.cvr.gluetools.programs.cdhit.CdHitEstRunner;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ModulePluginFactory extends PluginFactory<ModulePlugin<?>>{

	public static Multiton.Creator<ModulePluginFactory> creator = new
			Multiton.SuppliedCreator<>(ModulePluginFactory.class, ModulePluginFactory::new);
	
	private PluginGroupRegistry<ModulePluginGroup, ModulePlugin<?>> modulePluginGroupRegistry = 
			new PluginGroupRegistry<ModulePluginGroup, ModulePlugin<?>>(ModulePluginGroup.OTHER);
	
	private ModulePluginFactory() {
		super();
		setModulePluginGroup(new ModulePluginGroup("sequenceFasta", "Module types for working with sequences in FASTA format", 80));
		registerPluginClass(FastaImporter.class);
		registerPluginClass(FastaExporter.class);
		registerPluginClass(FastaProteinExporter.class);
		registerPluginClass(FastaSequenceReporter.class);		
		registerPluginClass(FastaUtility.class);
		
		setModulePluginGroup(new ModulePluginGroup("tabular", "Module types for working with tabular data", 81));
		registerPluginClass(TextFilePopulator.class);
		registerPluginClass(TabularUtility.class);
		
		setModulePluginGroup(new ModulePluginGroup("ncbi", "Module types for working with NCBI / GenBank sequence data", 82));
		registerPluginClass(NcbiImporter.class);
		registerPluginClass(GenbankXmlPopulator.class);
		registerPluginClass(GbSubmisisonGenerator.class);

		setModulePluginGroup(new ModulePluginGroup("aligners", "Module types for computing alignments", 83));
		registerPluginClass(BlastAligner.class);
		registerPluginClass(CodonAwareBlastAligner.class);
		registerPluginClass(CompoundAligner.class);
		registerPluginClass(MafftAligner.class);
		
		setModulePluginGroup(new ModulePluginGroup("alignmentFasta", "Module types for importing/exporting alignments", 84));
		registerPluginClass(FastaAlignmentImporter.class);
		registerPluginClass(BlastFastaAlignmentImporter.class);
		registerPluginClass(BlastFastaProteinAlignmentImporter.class);
		registerPluginClass(FastaAlignmentExporter.class);
		registerPluginClass(FastaProteinAlignmentExporter.class);
		registerPluginClass(AlignmentColumnsSelector.class);

		setModulePluginGroup(new ModulePluginGroup("consensus", "Module types for generating consensus sequences", 85));
		registerPluginClass(NucleotideConsensusGenerator.class);
		registerPluginClass(AminoAcidConsensusGenerator.class);

		setModulePluginGroup(new ModulePluginGroup("phylogenetics", "Module types for working with phylogenetics", 86));
		registerPluginClass(RaxmlPhylogenyGenerator.class);
		registerPluginClass(PhyloImporter.class);
		registerPluginClass(PhyloUtility.class);
		registerPluginClass(PhyloExporter.class);
		registerPluginClass(NexusExporter.class);
		registerPluginClass(TreeTransformer.class);
		registerPluginClass(MaxLikelihoodPlacer.class);
		// mothballed for the time being.
		// registerPluginClass(SimpleDistanceGenotyper.class);
		registerPluginClass(MaxLikelihoodGenotyper.class);
		registerPluginClass(FigTreeAnnotationExporter.class);
		registerPluginClass(ClusterPickerRunner.class);
		registerPluginClass(CdHitEstRunner.class);
		
		setModulePluginGroup(new ModulePluginGroup("scripting", "Module types for general-purpose scripting", 87));
		registerPluginClass(EcmaFunctionInvoker.class);
		registerPluginClass(FreemarkerTextToGlueTransformer.class);
		registerPluginClass(FreemarkerObjectRenderer.class);
		registerPluginClass(FreemarkerDocTransformer.class);
		registerPluginClass(TextToQueryTransformer.class);
		
		setModulePluginGroup(new ModulePluginGroup("deepSequencing", "Module types for working with deep sequencing data", 88));
		registerPluginClass(SamReporter.class);
		registerPluginClass(SamFileGenerator.class);
		
		
		setModulePluginGroup(new ModulePluginGroup("variations", "Module types for working with variations", 89));
		registerPluginClass(VariationFrequenciesGenerator.class);

		setModulePluginGroup(new ModulePluginGroup("translation", "Module types for working with protein translation", 90));
		registerPluginClass(TranslationModifier.class);

		
		
		
		setModulePluginGroup(new ModulePluginGroup("experimentalUnsupported", "Experimental / unsupported module types", 91));
		registerPluginClass(ModelTester.class);
		registerPluginClass(DigsImporter.class);
		registerPluginClass(GbRefBuilder.class);

		setModulePluginGroup(null); // OTHER
		registerPluginClass(BlastSequenceRotator.class);
		registerPluginClass(BlastSequenceRecogniser.class);
		registerPluginClass(AlignmentBasedSequenceMerger.class);
		registerPluginClass(Kuiken2006CodonLabeler.class);
		registerPluginClass(FeaturePresenceRecorder.class);

		setModulePluginGroup(new ModulePluginGroup("visualisation", "Visualisation module types", 92));
		registerPluginClass(VisualisationUtility.class);
		registerPluginClass(TreeVisualiser.class);
		
		setModulePluginGroup(new ModulePluginGroup("fineGrainedAminoAcid", "Module types for fine-grained amino acid analysis", 93));
		registerPluginClass(Hanada2006ReplacementClassifier.class);
		registerPluginClass(Grantham1974DistanceCalculator.class);
		registerPluginClass(Miyata1979DistanceCalculator.class);
	}

	
	
	@Override
	protected void registerPluginClassInternal(String elemName, PluginFactory<ModulePlugin<?>>.PluginClassInfo pluginClassInfo) {
		super.registerPluginClassInternal(elemName, pluginClassInfo);
		getModulePluginGroupRegistry().registerElemName(elemName);
	}


	public PluginGroupRegistry<ModulePluginGroup, ModulePlugin<?>> getModulePluginGroupRegistry() {
		return modulePluginGroupRegistry;
	}
	
	private void setModulePluginGroup(ModulePluginGroup modulePluginGroup) {
		modulePluginGroupRegistry.setPluginGroup(modulePluginGroup);
	}
	
	
	
}
