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
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamUtils;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;

@PluginClass(elemName="steppedReadSet")
public class SteppedReadSet extends BaseSamReadSet {

	private int read1Length;
	private int read2Length;
	private int gapSize; // negative implies the paired reads overlap.
	private int stepSize;
	private Integer refStartNt; // first pair will start at this reference NT -- defaults to 1
	private Integer numSteps; // if null then reads will be generated up to the end of the sequence.

	private String readNamePrefix;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.read1Length = PluginUtils.configureIntProperty(configElem, "read1Length", true);
		this.read2Length = PluginUtils.configureIntProperty(configElem, "read2Length", true);
		this.gapSize = PluginUtils.configureIntProperty(configElem, "gapSize", true);
		this.stepSize = PluginUtils.configureIntProperty(configElem, "stepSize", true);
		this.refStartNt = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "refStartNt", false)).orElse(1);
		this.numSteps = PluginUtils.configureIntProperty(configElem, "numSteps", false);
		this.readNamePrefix = PluginUtils.configureStringProperty(configElem, "readNamePrefix", true);

	}


	@Override
	public void writeReads(CommandContext cmdContext, SAMFileHeader samFileHeader, SamFileGenerator samFileGenerator, SAMFileWriter samFileWriter) {
		String mainReference = samFileGenerator.getMainReference();
		ReferenceSequence refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(mainReference));
		String refNTs = refSequence.getSequence().getSequenceObject().getNucleotides(cmdContext);
		
		int read1StartNt = refStartNt;
		int steps = 0;
		
		int readNameSuffix = 0;

		int refLength = refNTs.length();
		while(read1StartNt <= refLength || (numSteps == null || steps < numSteps)) {
			
			int read1EndNt = read1StartNt + read1Length - 1;
			int read2StartNt = read1EndNt + gapSize + 1;
			int read2EndNt = read2StartNt + read2Length - 1;
			
			if(read1EndNt > refLength || read2EndNt > refLength) {
				break;
			}
			
			String readName = readNamePrefix+Integer.toString(readNameSuffix);

			SAMRecord read1 = new SAMRecord(samFileHeader);
			read1.setReferenceName(mainReference);
			read1.setReadName(readName);
			read1.setReadString(deAmbiguizeNts(SegmentUtils.base1SubString(refNTs, read1StartNt, read1EndNt)));
			read1.setFirstOfPairFlag(true);
			read1.setProperPairFlag(true);
			read1.setReadPairedFlag(true);
			read1.setAlignmentStart(read1StartNt);
			applyRead1BaseQuality(samFileGenerator, read1);
			applyRead1MappingQuality(samFileGenerator, read1);
			read1.setCigar(new Cigar(Arrays.asList(new CigarElement(read1Length, CigarOperator.M))));
			samFileWriter.addAlignment(read1);

			SAMRecord read2 = new SAMRecord(samFileHeader);
			read2.setReferenceName(mainReference);
			read2.setReadName(readName);
			read2.setReadString(deAmbiguizeNts(SegmentUtils.base1SubString(refNTs, read2StartNt, read2EndNt)));
			read2.setSecondOfPairFlag(true);
			read2.setProperPairFlag(true);
			read2.setReadPairedFlag(true);
			read2.setAlignmentStart(read2StartNt);
			applyRead2BaseQuality(samFileGenerator, read2);
			applyRead2MappingQuality(samFileGenerator, read2);
			read2.setCigar(new Cigar(Arrays.asList(new CigarElement(read2Length, CigarOperator.M))));
			samFileWriter.addAlignment(read2);

			read1StartNt += stepSize;
			steps ++;
			readNameSuffix ++;
		}
	}
	
	
}
