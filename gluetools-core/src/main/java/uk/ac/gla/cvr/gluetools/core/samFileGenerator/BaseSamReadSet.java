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

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamUtils;
import uk.ac.gla.cvr.gluetools.core.translation.ResidueUtils;

public abstract class BaseSamReadSet implements Plugin {

	private Integer read1BaseQuality; 
	private Integer read1MappingQuality; 
	private Integer read2BaseQuality; 
	private Integer read2MappingQuality; 

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		this.read1BaseQuality = PluginUtils.configureIntProperty(configElem, "read1BaseQuality", false);
		this.read1MappingQuality = PluginUtils.configureIntProperty(configElem, "read1MappingQuality", false);
		this.read2BaseQuality = PluginUtils.configureIntProperty(configElem, "read2BaseQuality", false);
		this.read2MappingQuality = PluginUtils.configureIntProperty(configElem, "read2MappingQuality", false);
	}

	public abstract void writeReads(CommandContext cmdContext, SAMFileHeader samFileHeader, SamFileGenerator samFileGenerator, SAMFileWriter samFileWriter);

	protected String deAmbiguizeNts(String nts) {
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < nts.length(); i++) {
			int ambigNtInt = ResidueUtils.ambigNtToInt(nts.charAt(i));
			int[] concreteNtInts = ResidueUtils.ambigNtToConcreteNts(ambigNtInt);
			buf.append(ResidueUtils.intToConcreteNt(concreteNtInts[0]));
		}
		return buf.toString();
	}
	
	protected void applyRead1BaseQuality(SamFileGenerator samFileGenerator, SAMRecord read1) {
		if(read1BaseQuality != null) {
			read1.setBaseQualityString(formQualityString(read1.getReadLength(), read1BaseQuality));
		} else {
			read1.setBaseQualityString(formQualityString(read1.getReadLength(), samFileGenerator.getDefaultBaseQuality()));
		}
	}
	
	protected void applyRead2BaseQuality(SamFileGenerator samFileGenerator, SAMRecord read2) {
		if(read2BaseQuality != null) {
			read2.setBaseQualityString(formQualityString(read2.getReadLength(), read2BaseQuality));
		} else {
			read2.setBaseQualityString(formQualityString(read2.getReadLength(), samFileGenerator.getDefaultBaseQuality()));
		}
	}
	protected void applyRead1MappingQuality(SamFileGenerator samFileGenerator, SAMRecord read1) {
		if(read1MappingQuality != null) {
			read1.setMappingQuality(read1MappingQuality);
		} else {
			read1.setMappingQuality(samFileGenerator.getDefaultMappingQuality());
		}
	}
	protected void applyRead2MappingQuality(SamFileGenerator samFileGenerator, SAMRecord read2) {
		if(read2MappingQuality != null) {
			read2.setMappingQuality(read2MappingQuality);
		} else {
			read2.setMappingQuality(samFileGenerator.getDefaultMappingQuality());
		}
	}

	private String formQualityString(int readLength, int baseQuality) {
		StringBuffer qualityStringBuf = new StringBuffer(readLength);
		for(int i = 0; i < readLength; i++) {
			qualityStringBuf.append(SamUtils.qScoreToQualityChar(baseQuality));
		}
		String qualityString = qualityStringBuf.toString();
		return qualityString;
	}
	
}
