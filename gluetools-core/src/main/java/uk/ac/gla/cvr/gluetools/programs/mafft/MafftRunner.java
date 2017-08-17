package uk.ac.gla.cvr.gluetools.programs.mafft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
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

public class MafftRunner implements Plugin {

	public static final String GAP_OPENING_PENALTY = "gapOpeningPenalty";
	public static final String EXTENSION_PENALTY = "extensionPenalty";
	public static final String MAX_ITERATE = "maxIterate";
	
	private Double gapOpeningPenalty;
	private Double extensionPenalty;
	private Integer maxIterate;

	public enum Task {
		ADD,
		ADD_KEEPLENGTH
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
		
		String mafftTempDir = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(MafftUtils.MAFFT_TEMP_DIR_PROPERTY);
		if(mafftTempDir == null) { throw new MafftException(Code.MAFFT_CONFIG_EXCEPTION, "MAFFT temp directory not defined in config property "+MafftUtils.MAFFT_TEMP_DIR_PROPERTY); }

		String mafftExecutable = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(MafftUtils.MAFFT_EXECUTABLE_PROPERTY);
		if(mafftExecutable == null) { throw new MafftException(Code.MAFFT_CONFIG_EXCEPTION, "MAFFT executable not defined in config property "+MafftUtils.MAFFT_EXECUTABLE_PROPERTY); }

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

					Map<String, MafftSingleSequenceCallable> fastaIDToCallable = new LinkedHashMap<String, MafftSingleSequenceCallable>();

					query.forEach( (fastaID, sequence) -> {
						fastaIDToCallable.put(fastaID, new MafftSingleSequenceCallable(task, tempDir, mafftExecutable, uuid, alignmentFile, fastaID, sequence));
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
					mafftResult.setAlignmentWithQuery(alignmentWithQuery);
				} else {
					MafftMultiSequenceCallable callable = new MafftMultiSequenceCallable(task, tempDir, mafftExecutable, uuid, alignmentFile, query);
					callable.call();
					mafftResult.setAlignmentWithQuery(callable.getAlignmentWithQuery());
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
		private String mafftExecutable;
		private String uuid;
		private File alignmentFile;
		
		public MafftCallable(Task task, File tempDir, String mafftExecutable,
				String uuid, File alignmentFile) {
			super();
			this.task = task;
			this.tempDir = tempDir;
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
			commandWords.add(mafftExecutable);

			// threads / number of CPUs
			commandWords.add("--thread");
			commandWords.add(Integer.toString(1));

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

	}
	
	private class MafftSingleSequenceCallable extends MafftCallable implements Callable<Void> {

		private String fastaID;
		private DNASequence querySequence;
		private DNASequence alignmentRow;
		
		public MafftSingleSequenceCallable(Task task, File tempDir, String mafftExecutable,
				String uuid, File alignmentFile, String fastaID,
				DNASequence querySequence) {
			super(task, tempDir, mafftExecutable, uuid, alignmentFile);
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
	}
	
	
	private class MafftMultiSequenceCallable extends MafftCallable implements Callable<Void> {

		private Map<String, DNASequence> querySequences;
		private Map<String, DNASequence> alignmentWithQuery;
		
		public MafftMultiSequenceCallable(Task task, File tempDir, String mafftExecutable,
				String uuid, File alignmentFile, Map<String, DNASequence> querySequences) {
			super(task, tempDir, mafftExecutable, uuid, alignmentFile);
			this.querySequences = querySequences;
		}

		// returns single alignment row for query.
		@Override
		public Void call() throws Exception {
			File queryFile = new File(getTempDir(), getUuid()+"_query.fasta");
			writeFastaFile(getTempDir(), queryFile, querySequences);

			this.alignmentWithQuery = runMafft(queryFile);
			return null;
		}


		public Map<String, DNASequence> getAlignmentWithQuery() {
			return this.alignmentWithQuery;
		}
	}

	
	
}