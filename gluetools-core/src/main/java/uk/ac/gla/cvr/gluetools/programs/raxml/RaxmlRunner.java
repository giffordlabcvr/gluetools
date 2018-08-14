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
package uk.ac.gla.cvr.gluetools.programs.raxml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.modules.PropertyGroup;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlException.Code;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

public abstract class RaxmlRunner implements Plugin {

	public static final String SUBSTITUTION_MODEL = "substitutionModel";
	public static final String RANDOM_NUMBER_SEED_1 = "randomNumberSeed1";
	
	public enum SubstitutionModel {
		GTRCAT(true, false),
		GTRGAMMA(true, false),
		GTRGAMMAI(true, false),
		PROTCATGTR(false, true),
		PROTGAMMAGTR(false, true),
		PROTGAMMAIGTR(false, true);
		
		private boolean nucleotide;
		private boolean protein;
		
		private SubstitutionModel(boolean nucleotide, boolean protein) {
			this.nucleotide = nucleotide;
			this.protein = protein;
		}
		public boolean isNucleotide() {
			return nucleotide;
		}
		public boolean isProtein() {
			return protein;
		}
	}
	
	private SubstitutionModel substitutionModel = SubstitutionModel.GTRCAT;
	private Integer randomNumberSeed1 = 12345;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		substitutionModel = PluginUtils.configureEnumProperty(SubstitutionModel.class, configElem, SUBSTITUTION_MODEL, substitutionModel);
		randomNumberSeed1 = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, RANDOM_NUMBER_SEED_1, false)).orElse(randomNumberSeed1);
	}

	@Override
	public void configurePropertyGroup(PropertyGroup propertyGroup) {
		Plugin.super.configurePropertyGroup(propertyGroup);
		propertyGroup
			.addPropertyName(SUBSTITUTION_MODEL)
			.addPropertyName(RANDOM_NUMBER_SEED_1);
	}

	protected SubstitutionModel getSubstitutionModel() {
		return substitutionModel;
	}

	protected Integer getRandomNumberSeed1() {
		return randomNumberSeed1;
	}

	protected void writeAlignmentFile(File tempDir, File alignmentFile, Map<String, DNASequence> alignment) {
		byte[] fastaBytes = FastaUtils.mapToFasta(alignment, LineFeedStyle.LF);
		try(FileOutputStream fileOutputStream = new FileOutputStream(alignmentFile)) {
			IOUtils.write(fastaBytes, fileOutputStream);
		} catch (IOException e) {
			throw new RaxmlException(e, Code.RAXML_FILE_EXCEPTION, "Failed to write "+alignmentFile.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}

	protected int getRaxmlCpus(CommandContext cmdContext) {
		return Integer.parseInt(cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(RaxmlUtils.RAXMLHPC_NUMBER_CPUS, "1"));
	}

	protected String getRaxmlExecutable(CommandContext cmdContext) {
		String raxmlExecutable = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(RaxmlUtils.RAXMLHPC_EXECUTABLE_PROPERTY);
		if(raxmlExecutable == null) { throw new RaxmlException(Code.RAXML_CONFIG_EXCEPTION, "RAxML executable not defined in config property "+RaxmlUtils.RAXMLHPC_EXECUTABLE_PROPERTY); }
		return raxmlExecutable;
	}

	protected String getRaxmlTempDir(CommandContext cmdContext) {
		String raxmlTempDir = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(RaxmlUtils.RAXML_TEMP_DIR_PROPERTY);
		if(raxmlTempDir == null) { throw new RaxmlException(Code.RAXML_CONFIG_EXCEPTION, "RAxML temp directory not defined in config property "+RaxmlUtils.RAXML_TEMP_DIR_PROPERTY); }
		return raxmlTempDir;
	}

	protected void checkAlignment(Map<String, DNASequence> alignment) {
		for(String string: alignment.keySet()) {
			if(!RaxmlUtils.validRaxmlName(string)) {
				throw new RaxmlException(Code.RAXML_DATA_EXCEPTION, "Alignment contains row name \""+string+"\" which is invalid in RAxML");
			}
		}
	}

	protected String normalisedFilePath(File file) {
		String normalizedPath = file.getAbsolutePath();
		if(System.getProperty("os.name").toLowerCase().contains("windows")) {
			normalizedPath = normalizedPath.replace('\\', '/');
		}
		return normalizedPath;
	}

	
}
