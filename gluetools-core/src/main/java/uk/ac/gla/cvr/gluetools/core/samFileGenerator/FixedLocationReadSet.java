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
package uk.ac.gla.cvr.gluetools.core.samFileGenerator;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamUtils;
import uk.ac.gla.cvr.gluetools.core.samFileGenerator.SamFileGeneratorException.Code;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="fixedLocationReadSet")
public class FixedLocationReadSet extends BaseSamReadSet {

	private int read1Length;
	private int read2Length;
	private Integer refStartNt; 
	private String startFeature; 
	private String startCodonLabel; 
	private String readNamePrefix;
	private int numReads;
	private int gapSize;
	
	private int defaultBaseQuality = 50;
	private int mappingQuality = 38;
	
	private List<BaseReadPolymorphism> readPolymorphisms;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.numReads = PluginUtils.configureIntProperty(configElem, "numReads", true);
		this.read1Length = PluginUtils.configureIntProperty(configElem, "read1Length", true);
		this.read2Length = PluginUtils.configureIntProperty(configElem, "read2Length", true);
		this.refStartNt = PluginUtils.configureIntProperty(configElem, "refStartNt", false);
		this.startFeature = PluginUtils.configureStringProperty(configElem, "startFeature", false);
		this.startCodonLabel = PluginUtils.configureStringProperty(configElem, "startCodonLabel", false);
		this.gapSize = PluginUtils.configureIntProperty(configElem, "gapSize", true);
		this.readNamePrefix = PluginUtils.configureStringProperty(configElem, "readNamePrefix", true);

		ReadPolymorphismFactory readPolymorphismFactory = PluginFactory.get(ReadPolymorphismFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(readPolymorphismFactory.getElementNames());
		List<Element> polymorphismElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		this.readPolymorphisms = readPolymorphismFactory.createFromElements(pluginConfigContext, polymorphismElems);

		if(! (
				(refStartNt != null && startFeature == null && startCodonLabel == null) ||
				(refStartNt == null && startFeature != null && startCodonLabel != null)
				) ) {
			throw new SamFileGeneratorException(Code.CONFIG_ERROR, "Either refStartNt or both startFeature and startCodonLabel must be defined");
		}
		
	}


	@Override
	public void writeReads(CommandContext cmdContext, SAMFileHeader samFileHeader, SamFileGenerator samFileGenerator, SAMFileWriter samFileWriter) {
		String mainReference = samFileGenerator.getMainReference();
		ReferenceSequence refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(mainReference));
		String refNTs = refSequence.getSequence().getSequenceObject().getNucleotides(cmdContext);
		
		int read1StartNt;
		if(refStartNt != null) {
			read1StartNt = refStartNt;
		} else {
			String mainReferenceName = samFileGenerator.getMainReference();
			FeatureLocation featureLocation = 
					GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(mainReferenceName, startFeature), false);
			LabeledCodon labeledCodon = featureLocation.getLabelToLabeledCodon(cmdContext).get(startCodonLabel);
			read1StartNt = labeledCodon.getNtStart();
		}

		int read1EndNt = read1StartNt + read1Length - 1;
		int read2StartNt = read1EndNt + gapSize + 1;
		int read2EndNt = read2StartNt + read2Length - 1;

		for(int i = 0; i < numReads; i++) {
			
			String readName = readNamePrefix+Integer.toString(i);

			SAMRecord read1 = new SAMRecord(samFileHeader);
			read1.setReferenceName(mainReference);
			read1.setReadName(readName);
			read1.setReadString(deAmbiguizeNts(SegmentUtils.base1SubString(refNTs, read1StartNt, read1EndNt)));
			read1.setBaseQualityString(formQualityString(read1Length));
			read1.setFirstOfPairFlag(true);
			read1.setProperPairFlag(true);
			read1.setReadPairedFlag(true);
			read1.setAlignmentStart(read1StartNt);
			read1.setMappingQuality(mappingQuality);
			read1.setCigar(new Cigar(Arrays.asList(new CigarElement(read1Length, CigarOperator.M))));
			
			for(BaseReadPolymorphism readPolymorphism: readPolymorphisms) {
				if(readPolymorphism.getApplyToRead1()) {
					readPolymorphism.applyPolymorphism(cmdContext, read1, samFileGenerator);
				}
			}
			
			samFileWriter.addAlignment(read1);
			

			SAMRecord read2 = new SAMRecord(samFileHeader);
			read2.setReferenceName(mainReference);
			read2.setReadName(readName);
			read2.setReadString(deAmbiguizeNts(SegmentUtils.base1SubString(refNTs, read2StartNt, read2EndNt)));
			read2.setBaseQualityString(formQualityString(read2Length));
			read2.setSecondOfPairFlag(true);
			read2.setProperPairFlag(true);
			read2.setReadPairedFlag(true);
			read2.setAlignmentStart(read2StartNt);
			read2.setMappingQuality(mappingQuality);
			read2.setCigar(new Cigar(Arrays.asList(new CigarElement(read2Length, CigarOperator.M))));

			for(BaseReadPolymorphism readPolymorphism: readPolymorphisms) {
				if(readPolymorphism.getApplyToRead2()) {
					readPolymorphism.applyPolymorphism(cmdContext, read2, samFileGenerator);
				}
			}

			samFileWriter.addAlignment(read2);

		}
	}

	private String formQualityString(int readLength) {
		StringBuffer qualityStringBuf = new StringBuffer(readLength);
		for(int i = 0; i < readLength; i++) {
			qualityStringBuf.append(SamUtils.qScoreToQualityChar(defaultBaseQuality));
		}
		String qualityString = qualityStringBuf.toString();
		return qualityString;
	}
	
	
}
