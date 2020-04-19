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
package uk.ac.gla.cvr.gluetools.programs.mafft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.cygwin.CygwinUtils;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.mafft.MafftException.Code;
import uk.ac.gla.cvr.gluetools.programs.mafft.add.MafftResult;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

public class MafftRunner implements Plugin {

	public static final String GAP_OPENING_PENALTY = "gapOpeningPenalty";
	public static final String EXTENSION_PENALTY = "extensionPenalty";
	public static final String MAX_ITERATE = "maxIterate";
	
	private Double gapOpeningPenalty;
	private Double extensionPenalty;
	private Integer maxIterate;

	public enum Task {
		ADD,
		ADD_KEEPLENGTH,
		COMPUTE
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		gapOpeningPenalty = PluginUtils.configureDoubleProperty(configElem, GAP_OPENING_PENALTY, false);
		extensionPenalty = PluginUtils.configureDoubleProperty(configElem, EXTENSION_PENALTY, false);
		maxIterate = PluginUtils.configureIntProperty(configElem, MAX_ITERATE, false);
	}
	
	// independentQueries = true can be used to add each query in a separate mafft task
	// only works if Task is ADD_KEEPLENGTH
	
	public MafftResult executeMafft(CommandContext cmdContext, Task task, boolean independentQueries,
			Map<String, DNASequence> alignment, Map<String, DNASequence> query, File dataDirFile) {
		if(independentQueries && !task.equals(Task.ADD_KEEPLENGTH)) {
			throw new MafftException(Code.MAFFT_CONFIG_EXCEPTION, "MAFFT runner independentQueries option can only be used with ADD_KEEPLENGTH task");
		}
		
		if(query != null && !EnumSet.of(Task.ADD, Task.ADD_KEEPLENGTH).contains(task)) {
			throw new MafftException(Code.MAFFT_CONFIG_EXCEPTION, "MAFFT runner query can only be passed to ADD or ADD_KEEPLENGTH task");
		}

		// deals with some cases of empty input
		if(task.equals(Task.COMPUTE) && alignment.isEmpty()) {
			return MafftResult.emptyResult();
		}
		if(EnumSet.of(Task.ADD, Task.ADD_KEEPLENGTH).contains(task) && query.isEmpty()) {
			return MafftResult.fixedResult(alignment);
		}
		
		String mafftTempDir = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(MafftUtils.MAFFT_TEMP_DIR_PROPERTY);
		if(mafftTempDir == null) { throw new MafftException(Code.MAFFT_CONFIG_EXCEPTION, "MAFFT temp directory not defined in config property "+MafftUtils.MAFFT_TEMP_DIR_PROPERTY); }

		String mafftExecutable = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(MafftUtils.MAFFT_EXECUTABLE_PROPERTY);
		if(mafftExecutable == null) { throw new MafftException(Code.MAFFT_CONFIG_EXCEPTION, "MAFFT executable not defined in config property "+MafftUtils.MAFFT_EXECUTABLE_PROPERTY); }

		String cygwinShExecutable = null;
		if(System.getProperty("os.name").toLowerCase().contains("windows")) {
			cygwinShExecutable = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(CygwinUtils.CYGWIN_SH_EXECUTABLE_PROPERTY);
			if(cygwinShExecutable == null) { throw new MafftException(Code.MAFFT_CONFIG_EXCEPTION, "Cygwin sh executable not defined in config property "+CygwinUtils.CYGWIN_SH_EXECUTABLE_PROPERTY); }
		}
		
		String uuid = UUID.randomUUID().toString();
		File tempDir = new File(mafftTempDir, uuid);
			boolean mkdirsResult = tempDir.mkdirs();
			if((!mkdirsResult) || !(tempDir.exists() && tempDir.isDirectory())) {
				throw new MafftException(Code.MAFFT_FILE_EXCEPTION, "Failed to create MAFFT temporary directory: "+tempDir.getAbsolutePath());
			}
			File alignmentFile = new File(tempDir, "reference_alignment.fasta");
			writeFastaFile(tempDir, alignmentFile, alignment);

			MafftResult mafftResult = new MafftResult();
			
			try {
				if(independentQueries) {
					// For independent queries, (e.g. genotyping) we call mafft in separate java threads for each sequence.
					// This uses the mafftExecutorService, which has one worker per cpu as defined in gluetools.core.programs.mafft.cpus
					Map<String, MafftSingleSequenceCallable> fastaIDToCallable = new LinkedHashMap<String, MafftSingleSequenceCallable>();
					final String finalCygwinShExecutable = cygwinShExecutable;
					query.forEach( (fastaID, sequence) -> {
						fastaIDToCallable.put(fastaID, new MafftSingleSequenceCallable(task, tempDir, finalCygwinShExecutable, mafftExecutable, uuid, alignmentFile, fastaID, sequence));
					});

					ExecutorService mafftExecutorService = cmdContext.getGluetoolsEngine().getMafftExecutorService();
					List<Future<Void>> futures = mafftExecutorService.invokeAll(fastaIDToCallable.values());
					for(Future<Void> future: futures) { // pick up any exceptions.
						future.get(); 
					}
					Map<String, DNASequence> alignmentWithQuery = new LinkedHashMap<String, DNASequence>(alignment);
					fastaIDToCallable.forEach( (fastaID, callable) -> {
						alignmentWithQuery.put(fastaID, callable.getAlignmentRow());
					});
					mafftResult.setResultAlignment(alignmentWithQuery);
				} else {
					// for non-independent queries, (e.g. compute an unconstrained alignment) we call use single mafft with its number
					// of threads determined by gluetools.core.programs.mafft.cpus
					int mafftCpus = Integer.parseInt(cmdContext.getGluetoolsEngine()
							.getPropertiesConfiguration().getPropertyValue(MafftUtils.MAFFT_NUMBER_CPUS, "1"));

					
					MafftMultiSequenceCallable callable = new MafftMultiSequenceCallable(task, tempDir, cygwinShExecutable, mafftExecutable, 
							uuid, alignmentFile, query, mafftCpus);
					callable.call();
					mafftResult.setResultAlignment(callable.getResultAlignment());
				}

			} catch(Exception e) {
				throw new MafftException(e, Code.MAFFT_EXECUTION_EXCEPTION, e.getLocalizedMessage());
			} finally {
				boolean allFilesDeleted = true;
				if(tempDir != null && tempDir.exists() && tempDir.isDirectory()) {
					for(File file : tempDir.listFiles()) {
						if(dataDirFile != null) {
							byte[] fileBytes = ConsoleCommandContext.loadBytesFromFile(file);
							File fileToSave = new File(dataDirFile, file.getName());
							ConsoleCommandContext.saveBytesToFile(fileToSave, fileBytes);
						}
						boolean fileDeleteResult = file.delete();
						if(!fileDeleteResult) {
							GlueLogger.getGlueLogger().warning("Failed to delete temporary MAFFT file "+file.getAbsolutePath());
							allFilesDeleted = false;
							break;
						}
					}
					if(allFilesDeleted) {
						boolean dirDeleteResult = tempDir.delete();
						if(!dirDeleteResult) {
							GlueLogger.getGlueLogger().warning("Failed to delete temporary MAFFT directory "+tempDir.getAbsolutePath());
						}
					}
				}
			}
			
			return mafftResult;
	}

	private void writeFastaFile(File tempDir, File file, Map<String, DNASequence> alignment) {
		byte[] fastaBytes = FastaUtils.mapToFasta(alignment, LineFeedStyle.LF);
		try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
			IOUtils.write(fastaBytes, fileOutputStream);
		} catch (IOException e) {
			throw new MafftException(e, Code.MAFFT_FILE_EXCEPTION, "Failed to write "+file.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}
	
	private abstract class MafftCallable {

		private Task task;
		private File tempDir;
		private String cygwinShExecutable;
		private String mafftExecutable;
		private String uuid;
		private File alignmentFile;
		
		public MafftCallable(Task task, File tempDir, String cygwinShExecutable, String mafftExecutable,
				String uuid, File alignmentFile) {
			super();
			this.task = task;
			this.tempDir = tempDir;
			this.cygwinShExecutable = cygwinShExecutable;
			this.mafftExecutable = mafftExecutable;
			this.uuid = uuid;
			this.alignmentFile = alignmentFile;
		}
		
		protected File getTempDir() {
			return this.tempDir;
		}
		
		protected String getUuid() {
			return uuid;
		}
		
		public Map<String, DNASequence> runMafft(File queryFile) {
			List<String> commandWords = new ArrayList<String>();
			
			if(System.getProperty("os.name").toLowerCase().contains("windows")) {
				commandWords.add(cygwinShExecutable);
			}
			
			commandWords.add(mafftExecutable);

			// threads / number of CPUs
			commandWords.add("--thread");
			commandWords.add(Integer.toString(getNumCpus()));

			if(MafftRunner.this.gapOpeningPenalty != null) {
				commandWords.add("--op");
				commandWords.add(Double.toString(gapOpeningPenalty));
			}

			if(MafftRunner.this.extensionPenalty != null) {
				commandWords.add("--ep");
				commandWords.add(Double.toString(extensionPenalty));
			}

			if(MafftRunner.this.maxIterate != null) {
				commandWords.add("--maxiterate");
				commandWords.add(Integer.toString(maxIterate));
			}

			if(task == null) {
				throw new MafftException(Code.MAFFT_PROCESS_EXCEPTION, "MAFFT task was null");
			}

			switch(task) {
			case ADD:
				commandWords.add("--add");
				// query file
				commandWords.add(queryFile.getAbsolutePath());
				break;
			case ADD_KEEPLENGTH:
				commandWords.add("--add");
				// query file
				commandWords.add(queryFile.getAbsolutePath());
				commandWords.add("--keeplength");
				break;
			case COMPUTE:
				// in the compute case, query is not used, and the alignment file is the input sequences.
				break;
			default:
				throw new MafftException(Code.MAFFT_PROCESS_EXCEPTION, "Unknown MAFFT task "+task.name());
			}

			// alignment file
			commandWords.add(alignmentFile.getAbsolutePath());

			ProcessResult mafftProcessResult = ProcessUtils.runProcess(null, null, commandWords); 

			byte[] errorBytes = mafftProcessResult.getErrorBytes();
			if(mafftProcessResult.getExitCode() != 0 || 
					(errorBytes != null && 
					errorBytes.length > 0 && 
					(new String(errorBytes)).contains("ERROR"))) {
				GlueLogger.getGlueLogger().severe("MAFFT task "+getUuid()+", query failure, the MAFFT stdout was:");
				GlueLogger.getGlueLogger().severe(new String(mafftProcessResult.getOutputBytes()));
				GlueLogger.getGlueLogger().severe("MAFFT task "+getUuid()+", query  failure, the MAFFT stderr was:");
				GlueLogger.getGlueLogger().severe(new String(errorBytes));
				throw new MafftException(Code.MAFFT_PROCESS_EXCEPTION, "MAFFT task "+getUuid()+", query failed, see log for output/error content");
			}

			Map<String, DNASequence> alignmentWithQuery = FastaUtils.parseFasta(mafftProcessResult.getOutputBytes());
			return alignmentWithQuery;
		}

		protected abstract int getNumCpus();

	}
	
	private class MafftSingleSequenceCallable extends MafftCallable implements Callable<Void> {

		private String fastaID;
		private DNASequence querySequence;
		private DNASequence alignmentRow;
		
		public MafftSingleSequenceCallable(Task task, File tempDir, String cygwinShExecutable, String mafftExecutable,
				String uuid, File alignmentFile, String fastaID,
				DNASequence querySequence) {
			super(task, tempDir, cygwinShExecutable, mafftExecutable, uuid, alignmentFile);
			this.fastaID = fastaID;
			this.querySequence = querySequence;
		}

		// returns single alignment row for query.
		@Override
		public Void call() throws Exception {
			File queryFile = new File(getTempDir(), fastaID+"_query.fasta");
			Map<String, DNASequence> query = new LinkedHashMap<String, DNASequence>();
			query.put(fastaID, querySequence);
			writeFastaFile(getTempDir(), queryFile, query);

			Map<String, DNASequence> alignmentWithQuery = runMafft(queryFile);
			this.alignmentRow = alignmentWithQuery.get(fastaID);
			return null;
		}


		public DNASequence getAlignmentRow() {
			return alignmentRow;
		}

		@Override
		protected int getNumCpus() {
			return 1;
		}
	}
	
	
	private class MafftMultiSequenceCallable extends MafftCallable implements Callable<Void> {

		private Map<String, DNASequence> querySequences;
		private Map<String, DNASequence> resultAlignment;
		private int numCpus;
		
		public MafftMultiSequenceCallable(Task task, File tempDir, String cygwinShExecutable, String mafftExecutable,
				String uuid, File alignmentFile, Map<String, DNASequence> querySequences, int numCpus) {
			super(task, tempDir, cygwinShExecutable, mafftExecutable, uuid, alignmentFile);
			this.querySequences = querySequences;
			this.numCpus = numCpus;
		}

		// returns single alignment row for query.
		@Override
		public Void call() throws Exception {
			File queryFile = null;
			if(querySequences != null) {
				queryFile = new File(getTempDir(), getUuid()+"_query.fasta");
				writeFastaFile(getTempDir(), queryFile, querySequences);
			}
			this.resultAlignment = runMafft(queryFile);
			return null;
		}


		public Map<String, DNASequence> getResultAlignment() {
			return this.resultAlignment;
		}

		@Override
		protected int getNumCpus() {
			return numCpus;
		}
	}

	
	
}