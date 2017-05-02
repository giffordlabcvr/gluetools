package uk.ac.gla.cvr.gluetools.programs.jmodeltest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.modules.PropertyGroup;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.jmodeltest.JModelTestException.Code;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;

public class JModelTestRunner implements Plugin {

	public static final String NUM_SUBSTITUTION_SCHEMES = "numSubstitutionSchemes";
	public static final String INCLUDE_PROPORTION_INVARIABLE_SITES = "includeProportionInvariableSites";
	public static final String INCLUDE_UNEQUAL_BASE_FREQUENCIES = "includeUnequalBaseFrequencies";
	public static final String NUM_RATE_CATEGORIES = "numRateCategories";
	
	
	private Integer numSubstitutionSchemes;
	private Boolean includeProportionInvariableSites;
	private Integer numRateCategories;
	private Boolean includeUnequalBaseFrequencies;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		this.numSubstitutionSchemes = PluginUtils.configureIntProperty(configElem, NUM_SUBSTITUTION_SCHEMES, false);
		this.includeProportionInvariableSites = PluginUtils.configureBooleanProperty(configElem, INCLUDE_PROPORTION_INVARIABLE_SITES, false);
		this.includeUnequalBaseFrequencies = PluginUtils.configureBooleanProperty(configElem, INCLUDE_UNEQUAL_BASE_FREQUENCIES, false);
		this.numRateCategories = PluginUtils.configureIntProperty(configElem, NUM_RATE_CATEGORIES, false);
		if(numSubstitutionSchemes != null && !Arrays.asList(3, 5, 7, 11, 203).contains(numSubstitutionSchemes)) {
			throw new PluginConfigException(PluginConfigException.Code.CONFIG_CONSTRAINT_VIOLATION, "The <numSubstitutionSchemes> may be 3, 5, 7, 11 or 203");
		}
	}

	@Override
	public void configurePropertyGroup(PropertyGroup propertyGroup) {
		Plugin.super.configurePropertyGroup(propertyGroup);
		propertyGroup
			.addPropertyName(INCLUDE_PROPORTION_INVARIABLE_SITES)
			.addPropertyName(INCLUDE_UNEQUAL_BASE_FREQUENCIES)
			.addPropertyName(NUM_SUBSTITUTION_SCHEMES)
			.addPropertyName(NUM_RATE_CATEGORIES);
	}

	
	protected int getJModelTestCpus(CommandContext cmdContext) {
		return Integer.parseInt(cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(JModelTestUtils.JMODELTESTER_NUMBER_CPUS, "1"));
	}

	protected String getJModelTestJar(CommandContext cmdContext) {
		String jModelTestExecutable = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(JModelTestUtils.JMODELTESTER_JAR_PROPERTY);
		if(jModelTestExecutable == null) { throw new JModelTestException(JModelTestException.Code.JMODELTEST_CONFIG_EXCEPTION, "JModelTest executable not defined"); }
		return jModelTestExecutable;
	}

	protected String getJModelTestTempDir(CommandContext cmdContext) {
		String jModelTestTempDir = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(JModelTestUtils.JMODELTESTER_TEMP_DIR_PROPERTY);
		if(jModelTestTempDir == null) { throw new JModelTestException(JModelTestException.Code.JMODELTEST_CONFIG_EXCEPTION, "JModelTest temp directory not defined"); }
		return jModelTestTempDir;
	}

	public JModelTestResult runJModelTest(CommandContext cmdContext, Map<String, DNASequence> alignment, File dataDirFile) {
		String jModelTestTempDir = getJModelTestTempDir(cmdContext);
		String jModelTestJar = getJModelTestJar(cmdContext);
		int jModelTestCpus = getJModelTestCpus(cmdContext);
		
		checkAlignment(alignment);
		
		String sep = System.getProperty("file.separator");
		String osName = System.getProperty("os.name");
		
		String executableExtension = "";
		if(osName.startsWith("Windows")) {
			executableExtension = ".exe";
		}
		String javaExecutable = System.getProperty("java.home") + sep + "bin" + sep + "java" + executableExtension;
		
		String uuid = UUID.randomUUID().toString();
		File tempDir = new File(jModelTestTempDir, uuid);
		try {
			boolean mkdirsResult = tempDir.mkdirs();
			if((!mkdirsResult) || !(tempDir.exists() && tempDir.isDirectory())) {
				throw new JModelTestException(Code.JMODELTEST_FILE_EXCEPTION, "Failed to create jModelTest temporary directory: "+tempDir.getAbsolutePath());
			}
			File alignmentFile = new File(tempDir, "alignment.fasta");
			File outputFile = new File(tempDir, "output.txt");
			writeAlignmentFile(alignmentFile, alignment);
			
			List<String> commandWords = new ArrayList<String>();
			commandWords.add(javaExecutable);

			commandWords.add("-jar");

			commandWords.add(jModelTestJar);

			// input alignment 
			commandWords.add("-d");
			commandWords.add(alignmentFile.getAbsolutePath());

			// output file
			commandWords.add("-o");
			commandWords.add(outputFile.getAbsolutePath());

			// number of substitution schemes
			if(numSubstitutionSchemes != null) {
				commandWords.add("-s");
				commandWords.add(Integer.toString(numSubstitutionSchemes));
			}

			// Include models with unequal base frequencies
			if(includeUnequalBaseFrequencies != null && includeUnequalBaseFrequencies) {
				commandWords.add("-f");
			}

			// Include models with a of proportion invariable sites 
			if(includeProportionInvariableSites != null && includeProportionInvariableSites) {
				commandWords.add("-i");
			}

			// number of rate categories
			if(numRateCategories != null) {
				commandWords.add("-g");
				commandWords.add(Integer.toString(numRateCategories));
			}

			// threads / number of CPUs
			commandWords.add("-tr");
			commandWords.add(Integer.toString(jModelTestCpus));
			
			ProcessResult processResult = ProcessUtils.runProcess(null, tempDir, commandWords); 
			ProcessUtils.checkExitCode(commandWords, processResult);
			
			return new JModelTestResult();
		} finally {
			ProcessUtils.cleanUpTempDir(dataDirFile, tempDir);
		}
	}

	private void checkAlignment(Map<String, DNASequence> alignment) {
		for(String string: alignment.keySet()) {
			if(!JModelTestUtils.validPhyMLName(string)) {
				throw new JModelTestException(Code.JMODELTEST_FILE_EXCEPTION, "Alignment contains row name \""+string+"\" which is invalid in PhyML / jModelTest");
			}
		}
	}

	private void writeAlignmentFile(File alignmentFile, Map<String, DNASequence> alignment) {
		byte[] fastaBytes = FastaUtils.mapToFasta(alignment, LineFeedStyle.LF);
		try(FileOutputStream fileOutputStream = new FileOutputStream(alignmentFile)) {
			IOUtils.write(fastaBytes, fileOutputStream);
		} catch (IOException e) {
			throw new JModelTestException(e, Code.JMODELTEST_FILE_EXCEPTION, "Failed to write alignment file "+alignmentFile.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}



}
