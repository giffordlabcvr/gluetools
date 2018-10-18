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

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMSequenceRecord;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="samFileGenerator",
description="Generates a SAM/BAM file with known characteristics, for validation purposes")
public class SamFileGenerator extends ModulePlugin<SamFileGenerator> {

	public final static String MAIN_REFERENCE = "mainReference";
	public final static String DEFAULT_BASE_QUALITY = "defaultBaseQuality";
	public final static String DEFAULT_MAPPING_QUALITY = "defaultMappingQuality";
	
	private String mainReference;
	private List<BaseSamReadSet> samReadSets;

	private int defaultBaseQuality;
	private int defaultMappingQuality;

	
	public SamFileGenerator() {
		super();
		registerModulePluginCmdClass(GenerateBamFileCommand.class);
		registerModulePluginCmdClass(GenerateSamFileCommand.class);
	}



	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.mainReference = PluginUtils.configureStringProperty(configElem, MAIN_REFERENCE, true);
		this.defaultBaseQuality = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, DEFAULT_BASE_QUALITY, false)).orElse(50);
		this.defaultMappingQuality = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, DEFAULT_MAPPING_QUALITY, false)).orElse(38);
		
		SamReadSetFactory samReadSetFactory = PluginFactory.get(SamReadSetFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(samReadSetFactory.getElementNames());
		List<Element> setElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		this.samReadSets = samReadSetFactory.createFromElements(pluginConfigContext, setElems);
	}



	public SAMFileHeader generateHeader(CommandContext cmdContext) {
		SAMFileHeader header = new SAMFileHeader();
		ReferenceSequence refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(mainReference));
		String name = refSequence.getName();
		int length = refSequence.getSequence().getSequenceObject().getNucleotides(cmdContext).length();
		SAMSequenceRecord refSequenceRecord = new SAMSequenceRecord(name, length);
		header.addSequence(refSequenceRecord);
		return header;
	}

	public String getMainReference() {
		return mainReference;
	}

	public void writeReads(CommandContext cmdContext, SAMFileHeader samFileHeader, SAMFileWriter samFileWriter) {
		for(BaseSamReadSet samReadSet: samReadSets) {
			samReadSet.writeReads(cmdContext, samFileHeader, this, samFileWriter);
		}
	}

	public int getDefaultBaseQuality() {
		return defaultBaseQuality;
	}

	public int getDefaultMappingQuality() {
		return defaultMappingQuality;
	}
}
