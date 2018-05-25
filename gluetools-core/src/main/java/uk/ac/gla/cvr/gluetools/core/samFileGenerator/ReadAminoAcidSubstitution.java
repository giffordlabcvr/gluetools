package uk.ac.gla.cvr.gluetools.core.samFileGenerator;

import htsjdk.samtools.AlignmentBlock;
import htsjdk.samtools.SAMRecord;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.translation.CodonTableUtils;
import uk.ac.gla.cvr.gluetools.core.translation.ResidueUtils;

@PluginClass(elemName="aminoAcidSubstitution")
public class ReadAminoAcidSubstitution extends BaseReadPolymorphism {

	private String featureName;
	private String codonLabel;
	private String aminoAcid;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
		this.codonLabel = PluginUtils.configureStringProperty(configElem, "codonLabel", true);
		this.aminoAcid = PluginUtils.configureStringProperty(configElem, "aminoAcid", true);
	}
	
	@Override
	public void applyPolymorphism(CommandContext cmdContext, SAMRecord samRecord, SamFileGenerator samFileGenerator) {
		String mainReferenceName = samFileGenerator.getMainReference();
		FeatureLocation featureLocation = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(mainReferenceName, featureName), false);
		char[] readStringChars = samRecord.getReadString().toCharArray();

		LabeledCodon labeledCodon = featureLocation.getLabelToLabeledCodon(cmdContext).get(codonLabel);

		int labeledCodonRefNt = labeledCodon.getNtStart();
		
		int aaInt = ResidueUtils.aaToInt(aminoAcid.charAt(0));
		// arbitrarily pick the first nt triplet which codes for the AA.
		int[] substitutionNtInts = CodonTableUtils.aaToConcreteNtTriplets(aaInt).get(0);
		
		List<AlignmentBlock> alignmentBlocks = samRecord.getAlignmentBlocks();
		for(AlignmentBlock almtBlock: alignmentBlocks) {
			int almtBlockRefStart = almtBlock.getReferenceStart();
			int almtBlockRefEnd = almtBlockRefStart + almtBlock.getLength() - 1;
			int almtBlockRefToReadOffset = almtBlock.getReadStart() - almtBlockRefStart;
			// for each nt in the codon
			for(int i = 0; i < 3; i++) {
				int refNt = labeledCodonRefNt+i;
				if(almtBlockRefStart <= refNt && almtBlockRefEnd >= refNt) {
					int readNt = refNt + almtBlockRefToReadOffset;
					char substitutionNtChar = ResidueUtils.intToConcreteNt(substitutionNtInts[i]);
					readStringChars[readNt-1] = substitutionNtChar;
				}
			}
		}
		samRecord.setReadString(new String(readStringChars));
	}

	
}
