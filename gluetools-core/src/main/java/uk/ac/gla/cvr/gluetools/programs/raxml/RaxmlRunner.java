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
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlException.Code;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public abstract class RaxmlRunner implements Plugin {

	public static final String SUBSTITUTION_MODEL = "substitutionModel";
	public static final String RANDOM_NUMBER_SEED_1 = "randomNumberSeed1";
	
	private String substitutionModel = "GTRCAT";
	private Integer randomNumberSeed1 = 12345;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		substitutionModel = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, SUBSTITUTION_MODEL, false)).orElse(substitutionModel);
		randomNumberSeed1 = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, RANDOM_NUMBER_SEED_1, false)).orElse(randomNumberSeed1);
	}

	protected String getSubstitutionModel() {
		return substitutionModel;
	}

	protected Integer getRandomNumberSeed1() {
		return randomNumberSeed1;
	}

	protected void writeAlignmentFile(File tempDir, File alignmentFile, Map<String, DNASequence> alignment) {
		byte[] fastaBytes = FastaUtils.mapToFasta(alignment);
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
		if(raxmlExecutable == null) { throw new RaxmlException(Code.RAXML_CONFIG_EXCEPTION, "RAxML executable not defined"); }
		return raxmlExecutable;
	}

	protected String getRaxmlTempDir(CommandContext cmdContext) {
		String raxmlTempDir = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(RaxmlUtils.RAXML_TEMP_DIR_PROPERTY);
		if(raxmlTempDir == null) { throw new RaxmlException(Code.RAXML_CONFIG_EXCEPTION, "RAxML temp directory not defined"); }
		return raxmlTempDir;
	}

	protected void checkAlignment(Map<String, DNASequence> alignment) {
		for(String string: alignment.keySet()) {
			if(!RaxmlUtils.validRaxmlName(string)) {
				throw new RaxmlException(Code.RAXML_DATA_EXCEPTION, "Alignment contains row name \""+string+"\" which is invalid in RAxML");
			}
		}
	}

	
}
