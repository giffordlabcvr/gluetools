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
	
	public MafftResult executeMafft(CommandContext cmdContext, Task task, Map<String, DNASequence> alignment, Map<String, DNASequence> query, File dataDirFile) {

		String mafftTempDir = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(MafftUtils.MAFFT_TEMP_DIR_PROPERTY);
		if(mafftTempDir == null) { throw new MafftException(Code.MAFFT_CONFIG_EXCEPTION, "MAFFT temp directory not defined"); }

		String mafftExecutable = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(MafftUtils.MAFFT_EXECUTABLE_PROPERTY);
		if(mafftExecutable == null) { throw new MafftException(Code.MAFFT_CONFIG_EXCEPTION, "MAFFT executable not defined"); }

		String uuid = UUID.randomUUID().toString();
		File tempDir = new File(mafftTempDir, uuid);
			boolean mkdirsResult = tempDir.mkdirs();
			if((!mkdirsResult) || !(tempDir.exists() && tempDir.isDirectory())) {
				throw new MafftException(Code.MAFFT_FILE_EXCEPTION, "Failed to create MAFFT temporary directory: "+tempDir.getAbsolutePath());
			}
			File alignmentFile = new File(tempDir, "reference_alignment.fasta");
			writeFastaFile(tempDir, alignmentFile, alignment);

			MafftResult mafftResult = new MafftResult();
			Map<String, DNASequence> alignmentWithQuery = new LinkedHashMap<String, DNASequence>(alignment);
			mafftResult.setAlignmentWithQuery(alignmentWithQuery);
			
			try {
				
				Map<String, MafftCallable> fastaIDToCallable = new LinkedHashMap<String, MafftCallable>();
				
				query.forEach( (fastaID, sequence) -> {
					fastaIDToCallable.put(fastaID, new MafftCallable(task, tempDir, mafftExecutable, uuid, alignmentFile, fastaID, sequence));
				});
				
				ExecutorService mafftExecutorService = cmdContext.getGluetoolsEngine().getMafftExecutorService();
				List<Future<Void>> futures = mafftExecutorService.invokeAll(fastaIDToCallable.values());
				for(Future<Void> future: futures) { // pick up any exceptions.
					future.get(); 
				}
				fastaIDToCallable.forEach( (fastaID, callable) -> {
					alignmentWithQuery.put(fastaID, callable.getAlignmentRow());
				});

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
		byte[] fastaBytes = FastaUtils.mapToFasta(alignment);
		try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
			IOUtils.write(fastaBytes, fileOutputStream);
		} catch (IOException e) {
			throw new MafftException(e, Code.MAFFT_FILE_EXCEPTION, "Failed to write "+file.getAbsolutePath()+": "+e.getLocalizedMessage());
		}
	}
	
	private class MafftCallable implements Callable<Void> {

		private Task task;
		private File tempDir;
		private String mafftExecutable;
		private String uuid;
		private File alignmentFile;
		private String fastaID;
		private DNASequence querySequence;
		
		private DNASequence alignmentRow;
		
		public MafftCallable(Task task, File tempDir, String mafftExecutable,
				String uuid, File alignmentFile, String fastaID,
				DNASequence querySequence) {
			super();
			this.task = task;
			this.tempDir = tempDir;
			this.mafftExecutable = mafftExecutable;
			this.uuid = uuid;
			this.alignmentFile = alignmentFile;
			this.fastaID = fastaID;
			this.querySequence = querySequence;
		}

		// returns single alignment row for query.
		@Override
		public Void call() throws Exception {
			File queryFile = new File(tempDir, fastaID+"_query.fasta");
			Map<String, DNASequence> query = new LinkedHashMap<String, DNASequence>();
			query.put(fastaID, querySequence);
			writeFastaFile(tempDir, queryFile, query);

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
				GlueLogger.getGlueLogger().severe("MAFFT task "+uuid+", query "+fastaID+" failure, the MAFFT stdout was:");
				GlueLogger.getGlueLogger().severe(new String(mafftProcessResult.getOutputBytes()));
				GlueLogger.getGlueLogger().severe("MAFFT task "+uuid+", query "+fastaID+" failure, the MAFFT stderr was:");
				GlueLogger.getGlueLogger().severe(new String(errorBytes));
				throw new MafftException(Code.MAFFT_PROCESS_EXCEPTION, "MAFFT task "+uuid+", query "+fastaID+" failed, see log for output/error content");
			}

			Map<String, DNASequence> alignmentWithQuery = FastaUtils.parseFasta(mafftProcessResult.getOutputBytes());
			this.alignmentRow = alignmentWithQuery.get(fastaID);
			return null;
		}

		public DNASequence getAlignmentRow() {
			return alignmentRow;
		}
	}
}