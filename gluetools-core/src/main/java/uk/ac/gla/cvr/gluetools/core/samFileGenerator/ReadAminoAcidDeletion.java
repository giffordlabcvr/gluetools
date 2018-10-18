package uk.ac.gla.cvr.gluetools.core.samFileGenerator;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="aminoAcidDeletion")
public class ReadAminoAcidDeletion extends BaseReadPolymorphism {

	private String featureName;
	private String deletedCodonLabel;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
		this.deletedCodonLabel = PluginUtils.configureStringProperty(configElem, "deletedCodonLabel", true);
	}

	@Override
	public void applyPolymorphism(CommandContext cmdContext, SAMRecord samRecord, SamFileGenerator samFileGenerator) {
		String mainReferenceName = samFileGenerator.getMainReference();
		FeatureLocation featureLocation = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(mainReferenceName, featureName), false);
		LabeledCodon labeledCodon = featureLocation.getLabelToLabeledCodon(cmdContext).get(deletedCodonLabel);
		int deletedCodonRefNt = labeledCodon.getNtStart();
		
		char[] oldReadStringChars = samRecord.getReadString().toCharArray();
		char[] oldReadQualityChars = samRecord.getBaseQualityString().toCharArray();
		
		StringBuffer newReadStringChars = new StringBuffer();
		StringBuffer newReadQualityChars = new StringBuffer();
		
		Cigar oldCigar = samRecord.getCigar();
		List<CigarElement> oldCigarElements = oldCigar.getCigarElements();
		List<CigarElement> newCigarElements = new ArrayList<CigarElement>();

		int refNt = samRecord.getAlignmentStart();
		int readNt = 1;

		for(CigarElement oldCigarElement: oldCigarElements) {
			CigarOperator operator = oldCigarElement.getOperator();
			switch(operator) {
			case M:
				for(int i = 0; i < oldCigarElement.getLength(); i++) {
					if(refNt >= deletedCodonRefNt && refNt <= deletedCodonRefNt+2) {
						newCigarElements.add(new CigarElement(1, CigarOperator.D));
					} else {
						newCigarElements.add(new CigarElement(1, CigarOperator.M));
						newReadStringChars.append(oldReadStringChars[readNt-1]);
						newReadQualityChars.append(oldReadQualityChars[readNt-1]);
					}
					refNt++;
					readNt++;
				}
				break;
			case D:
				for(int i = 0; i < oldCigarElement.getLength(); i++) {
					newCigarElements.add(new CigarElement(1, CigarOperator.D));
					refNt++;
				}
			default: 
				throw new RuntimeException("Unhandled CIGAR element type "+operator.name());
			}
		}

		// rationalise cigar
		List<CigarElement> rationalisedCigarElements = new ArrayList<CigarElement>();
		if(newCigarElements.size() > 0) {
			CigarElement prevElem = newCigarElements.get(0);
			for(int i = 1; i < newCigarElements.size(); i++) {
				CigarElement currentElem = newCigarElements.get(i);
				if(currentElem.getOperator() == prevElem.getOperator()) {
					prevElem = new CigarElement(currentElem.getLength()+prevElem.getLength(), currentElem.getOperator());
				} else {
					rationalisedCigarElements.add(prevElem);
					prevElem = currentElem;
				}
			}
			rationalisedCigarElements.add(prevElem);
		}
		
		samRecord.setReadString(newReadStringChars.toString());
		samRecord.setBaseQualityString(newReadQualityChars.toString());
		Cigar rationalisedCigar = new Cigar(rationalisedCigarElements);
		samRecord.setCigar(rationalisedCigar);
	}

}
