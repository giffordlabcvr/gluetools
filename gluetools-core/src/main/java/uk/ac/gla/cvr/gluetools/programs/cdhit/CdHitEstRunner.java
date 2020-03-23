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
package uk.ac.gla.cvr.gluetools.programs.cdhit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.cdhit.CdHitException.Code;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@PluginClass(elemName="cdHitEstRunner",
description="Uses CD-HIT EST to quickly generate clusters of similar sequences")
public class CdHitEstRunner extends ModulePlugin<CdHitEstRunner> {

	public static final String SEQUENCE_IDENTITY_THRESHOLD = "sequenceIdentityThreshold";
	
	private Double sequenceIdentityThreshold = 0.9;

	
	
	public CdHitEstRunner() {
		super();
		addSimplePropertyName(SEQUENCE_IDENTITY_THRESHOLD);
		registerModulePluginCmdClass(CdHitEstGenerateClustersAlignmentCommand.class);
	}



	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sequenceIdentityThreshold = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, SEQUENCE_IDENTITY_THRESHOLD, false)).orElse(sequenceIdentityThreshold);
	}


	
	protected void writeInputSeqsFile(File tempDir, File inputSeqsFile, byte[] inputSeqsFastaBytes) {
		try(FileOutputStream fileOutputStream = new FileOutputStream(inputSeqsFile)) {
			IOUtils.write(inputSeqsFastaBytes, fileOutputStream);
		} catch (IOException e) {
			throw new CdHitException(e, Code.CDHIT_FILE_EXCEPTION, "Failed to write "+inputSeqsFile.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}

	protected int getCdHitEstCpus(CommandContext cmdContext) {
		return Integer.parseInt(cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(CdHitUtils.CDHIT_EST_NUMBER_CPUS, "1"));
	}

	protected String getCdHitEstExecutable(CommandContext cmdContext) {
		String cdHitEstExecutable = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(CdHitUtils.CDHIT_EST_EXECUTABLE_PROPERTY);
		if(cdHitEstExecutable == null) { throw new CdHitException(Code.CDHIT_CONFIG_EXCEPTION, "CD-HIT EST executable not defined in config property "+CdHitUtils.CDHIT_EST_EXECUTABLE_PROPERTY); }
		return cdHitEstExecutable;
	}

	protected String getCdHitEstTempDir(CommandContext cmdContext) {
		String cdHitEstTempDir = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(CdHitUtils.CDHIT_EST_TEMP_DIR_PROPERTY);
		if(cdHitEstTempDir == null) { throw new CdHitException(Code.CDHIT_CONFIG_EXCEPTION, "CD-HIT EST temp directory not defined in config property "+CdHitUtils.CDHIT_EST_TEMP_DIR_PROPERTY); }
		return cdHitEstTempDir;
	}

	protected String normalisedFilePath(File file) {
		String normalizedPath = file.getAbsolutePath();
		if(System.getProperty("os.name").toLowerCase().contains("windows")) {
			normalizedPath = normalizedPath.replace('\\', '/');
		}
		return normalizedPath;
	}

	
	public CdHitResult executeCdHitEst(CommandContext cmdContext, Map<String, DNASequence> inputSeqs, File dataDirFile) {

		String cdHitEstTempDir = getCdHitEstTempDir(cmdContext);
		String cdHitEstExecutable = getCdHitEstExecutable(cmdContext);
		int cdHitEstCpus = getCdHitEstCpus(cmdContext);
		
		Map<String, DNASequence> inputSeqsNoGaps = new LinkedHashMap<String, DNASequence>();
		Map<String, String> inputIdToCdHitId = new LinkedHashMap<String, String>();
		Map<String, String> cdHitIdToInputId = new LinkedHashMap<String, String>();
		
		int cdHitIdIdx = 1;
		
		for(Map.Entry<String, DNASequence> entry: inputSeqs.entrySet()) {
			String inputId = entry.getKey();
			String cdHitId = "S"+cdHitIdIdx;
			cdHitIdIdx++;
			inputIdToCdHitId.put(inputId, cdHitId);
			log(Level.FINEST, "Input ID "+inputId+" mapped to "+cdHitId);
			cdHitIdToInputId.put(cdHitId, inputId);
			inputSeqsNoGaps.put(cdHitId, new DNASequence(entry.getValue().getSequenceAsString().replace("-", "")));
		}
		
		byte[] inputSeqsFastaBytes = FastaUtils.mapToFasta(inputSeqsNoGaps, LineFeedStyle.LF);

		
		String uuid = UUID.randomUUID().toString();
		File tempDir = new File(cdHitEstTempDir, uuid);
		try {
			boolean mkdirsResult = tempDir.mkdirs();
			if((!mkdirsResult) || !(tempDir.exists() && tempDir.isDirectory())) {
				throw new CdHitException(Code.CDHIT_FILE_EXCEPTION, "Failed to create CD-HIT EST temporary directory: "+tempDir.getAbsolutePath());
			}

			File inputSeqsFile = new File(tempDir, "inputSeqs.fasta");
			writeInputSeqsFile(tempDir, inputSeqsFile, inputSeqsFastaBytes);
			
			List<String> commandWords = new ArrayList<String>();
			commandWords.add(cdHitEstExecutable);

			// inputSeqs file
			commandWords.add("-i");
			commandWords.add(normalisedFilePath(inputSeqsFile));

			// sequence identity threshold
			commandWords.add("-c");
			commandWords.add(Double.toString(sequenceIdentityThreshold));

			// threads / number of CPUs
			commandWords.add("-T");
			commandWords.add(Integer.toString(cdHitEstCpus));

			// output file
			File outputFile = new File(tempDir, "output");
			commandWords.add("-o");
			commandWords.add(normalisedFilePath(outputFile));

			
			ProcessResult cdHitEstProcessResult = ProcessUtils.runProcess(null, tempDir, commandWords); 

			ProcessUtils.checkExitCode(commandWords, cdHitEstProcessResult);

			return resultObjectFromTempDir(tempDir, cdHitIdToInputId);
		} finally {
			ProcessUtils.cleanUpTempDir(dataDirFile, tempDir);
		}
	}

	private CdHitResult resultObjectFromTempDir(File tempDir, Map<String, String> cdHitIdToInputId) {
		CdHitResult cdHitEstResult = new CdHitResult();
		File clstrFile = new File(tempDir, "output.clstr");
		byte[] clstrBytes;
		try(FileInputStream fileInputStream = new FileInputStream(clstrFile)) {
			clstrBytes = IOUtils.toByteArray(fileInputStream);
		} catch (IOException e) {
			throw new CdHitException(Code.CDHIT_FILE_EXCEPTION, "Failed to read RAxML output file: "+clstrFile.getAbsolutePath());
		}
		String clstrString = new String(clstrBytes);
		String[] clstrLines = clstrString.split("\\r?\\n");
		
		Integer currentClusterNum = null;
		String currentClusterRepresentative = null;
		List<String> currentClusterOthers = new ArrayList<String>();
		
		for(String clstrLine: clstrLines) {
			String trimmedLine = clstrLine.trim();
			if(trimmedLine.isEmpty()) {
				continue;
			} else if(trimmedLine.startsWith(">")) {
				if(currentClusterNum != null) {
					cdHitEstResult.addCluster(
						new CdHitCluster(currentClusterNum, currentClusterRepresentative, currentClusterOthers));
				}
				currentClusterNum = Integer.parseInt(trimmedLine.replace(">Cluster", "").trim());
				currentClusterRepresentative = null;
				currentClusterOthers = new ArrayList<String>();
			} else {
				String cdHitId = trimmedLine.substring(trimmedLine.indexOf(">")+1, trimmedLine.indexOf("..."));
				String inputId = cdHitIdToInputId.get(cdHitId);
				if(trimmedLine.endsWith("*")) {
					currentClusterRepresentative = inputId;
				} else {
					currentClusterOthers.add(inputId);
				}
			}
		}
		if(currentClusterNum != null) {
			cdHitEstResult.addCluster(
				new CdHitCluster(currentClusterNum, currentClusterRepresentative, currentClusterOthers));
		}
		return cdHitEstResult;
	}







	
	
}
