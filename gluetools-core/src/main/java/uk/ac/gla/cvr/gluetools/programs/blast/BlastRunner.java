package uk.ac.gla.cvr.gluetools.programs.blast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastException.Code;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResultBuilder.BlastXPath;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDB;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlException;
import uk.ac.gla.cvr.gluetools.programs.raxml.RaxmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;

public class BlastRunner implements Plugin {

	public enum BlastType {
		BLASTN,
		TBLASTN
	}
	
	
	public static String 
		BLASTN_EXECUTABLE_PROPERTY = "gluetools.core.programs.blast.blastn.executable"; 
	public static String 
		TBLASTN_EXECUTABLE_PROPERTY = "gluetools.core.programs.blast.tblastn.executable"; 
	public static String 
		BLAST_TEMP_DIR_PROPERTY = "gluetools.core.programs.blast.temp.dir"; 
	public static String 
		BLAST_SEARCH_THREADS_PROPERTY = "gluetools.core.programs.blast.search.threads"; 


	@SuppressWarnings("rawtypes")
	private List<Option> numericCommandLineOptions = new ArrayList<Option>();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		// general search options
		Element generalSearchElem = PluginUtils.findConfigElement(configElem, "generalSearch");
		if(generalSearchElem != null) {
			addDoubleOption(generalSearchElem, "evalue");
			addIntOption(generalSearchElem, "word_size", 4, true, null, false);
			addIntOption(generalSearchElem, "gapopen");
			addIntOption(generalSearchElem, "gapextend");
			addIntOption(generalSearchElem, "penalty", null, false, 0, true);
			addIntOption(generalSearchElem, "reward", 0, true, null, false);
		}
		// options to restrict search or results
		Element restrictElem = PluginUtils.findConfigElement(configElem, "restrictSearchOrResults");
		if(restrictElem != null) {
			addDoubleOption(restrictElem, "perc_identity", 0.0, true, 100.0, true);
			addDoubleOption(restrictElem, "qcov_hsp_perc", 0.0, true, 100.0, true);
			addIntOption(restrictElem, "max_hsps", 1, true, null, false);
			addIntOption(restrictElem, "culling_limit", 0, true, null, false);
			addDoubleOption(restrictElem, "best_hit_overhang", 0.0, false, 0.5, false);
			addDoubleOption(restrictElem, "best_hit_score_edge", 0.0, false, 0.5, false);
			addIntOption(restrictElem, "max_target_seqs", 1, true, null, false);
		}
	}

	public List<BlastResult> executeBlastUsingTempDir(CommandContext cmdContext, BlastType blastType, BlastDB blastDB, byte[] fastaBytes) {
	
		String blastTempDir = getBlastTempDir(cmdContext);
		String uuid = UUID.randomUUID().toString();
		File tempDir = new File(blastTempDir, uuid);
		File inputFile = new File(tempDir, "input");
		File outputFile = new File(tempDir, "output");
		File errorFile = new File(tempDir, "error");

		byte[] errorBytes;
		List<Document> resultDocs = new ArrayList<Document>();
		try {
			boolean mkdirsResult = tempDir.mkdirs();
			if ((!mkdirsResult) || !(tempDir.exists() && tempDir.isDirectory())) {
				throw new BlastException(Code.BLAST_FILE_EXCEPTION,
						"Failed to create BLAST temporary directory: "
								+ tempDir.getAbsolutePath());
			}

			try {
				blastDB.readLock().lock();
				ConsoleCommandContext.saveBytesToFile(inputFile, fastaBytes);

				PropertiesConfiguration propsConfig = cmdContext
						.getGluetoolsEngine().getPropertiesConfiguration();
				List<String> commandWords = constructCommandWords(cmdContext,
						propsConfig, blastType, blastDB);
				commandWords.add("-query");
				commandWords.add(inputFile.getAbsolutePath());
				commandWords.add("-out");
				commandWords.add(outputFile.getAbsolutePath());
				commandWords.add("-logfile");
				commandWords.add(errorFile.getAbsolutePath());

				// run blast based on the ref DB.
				ProcessUtils.runProcess(null, null, commandWords);
			} finally {
				blastDB.readLock().unlock();
			}
			if(errorFile.exists()) {
				errorBytes = ConsoleCommandContext.loadBytesFromFile(errorFile);
			} else {
				errorBytes = new byte[0];
			}

			String[] outputXmlFiles = tempDir.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith("output") && name.endsWith(".xml");
				}
			});
			try {
				for (String outputXmlFile : outputXmlFiles) {
					byte[] xmlBytes = ConsoleCommandContext
							.loadBytesFromFile(new File(tempDir, outputXmlFile));
					resultDocs.add(GlueXmlUtils.documentFromBytes(xmlBytes));
				}
				return constructBlastResults(resultDocs);
			} catch(Exception e) {
				GlueLogger.log(Level.FINE, "BLAST stderr:\n"+new String(errorBytes));
				if(e instanceof GlueException) {
					throw e;
				}
				throw new BlastException(BlastException.Code.BLAST_OUTPUT_FORMAT_ERROR, e.getLocalizedMessage());
			}
		} finally {
			ProcessUtils.cleanUpTempDir(null, tempDir);
		}

	}
	
	public List<BlastResult> executeBlast(CommandContext cmdContext, BlastType blastType, BlastDB blastDB, byte[] fastaBytes) {
		if(System.getProperty("os.name").toLowerCase().contains("windows")) {
			return executeBlastUsingTempDir(cmdContext, blastType, blastDB, fastaBytes);
		} else {
			return executeBlastUsingStreams(cmdContext, blastType, blastDB, fastaBytes);
		}
	}	

	@SuppressWarnings("rawtypes")
	public List<BlastResult> executeBlastUsingStreams(CommandContext cmdContext, BlastType blastType, BlastDB blastDB, byte[] fastaBytes) {
		blastDB.readLock().lock();
		ProcessResult blastProcessResult;
		try {
			PropertiesConfiguration propsConfig = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
			List<String> commandWords = constructCommandWords(cmdContext, propsConfig, blastType, blastDB);
			// run blast based on the ref DB.
			blastProcessResult = ProcessUtils.runProcess(new ByteArrayInputStream(fastaBytes), null, commandWords); 
		} finally {
			blastDB.readLock().unlock();
		}
		List<Document> resultDocs;
		try {
			resultDocs = GlueXmlUtils.documentsFromBytes(blastProcessResult.getOutputBytes());
			return constructBlastResults(resultDocs);
		} catch(Exception e) {
			GlueLogger.log(Level.FINE, "BLAST stderr:\n"+new String(blastProcessResult.getErrorBytes()));
			if(e instanceof GlueException) {
				throw e;
			}
			throw new BlastException(BlastException.Code.BLAST_OUTPUT_FORMAT_ERROR, e.getLocalizedMessage());
		}

	}

	private List<String> constructCommandWords(CommandContext cmdContext,
			PropertiesConfiguration propsConfig, BlastType blastType, BlastDB blastDB) {
		String blastExecutable = establishBlastType(blastType, propsConfig);
		Integer blastSearchThreads = establishSearchThreads(propsConfig);

		List<String> commandWords = new ArrayList<String>();
		commandWords.add(blastExecutable);
		// supply reference DB
		commandWords.add("-db");
		commandWords.add(new File(blastDB.getBlastDbDir(cmdContext), BlastDbManager.BLAST_DB_PREFIX).getAbsolutePath());
		// outfmt 14 is XML2
		commandWords.add("-outfmt");
		commandWords.add("14");
		for(Option numericOption : numericCommandLineOptions) {
			commandWords.add("-"+numericOption.getName());
			commandWords.add(numericOption.getValue().toString());
		}
		if(blastSearchThreads != null) {
			commandWords.add("-num_threads");
			commandWords.add(Integer.toString(blastSearchThreads));
		}
		return commandWords;
	}

	private List<BlastResult> constructBlastResults(List<Document> resultDocs) throws GlueException {
		// use same xPathEngine across results, should save some init time.
		// guess a faster way may be to use a SAX parser.
		BlastXPath blastXPath = new BlastXPath();
		List<BlastResult> blastResults = new ArrayList<BlastResult>();
		for(Document resultDoc: resultDocs) {
			blastResults.add(BlastResultBuilder.blastResultFromDocument(blastXPath, resultDoc));
		}
		return blastResults;
	}

	private Integer establishSearchThreads(PropertiesConfiguration propsConfig) {
		Integer blastSearchThreads = null;
		String blastSearchThreadsString = propsConfig.getPropertyValue(BLAST_SEARCH_THREADS_PROPERTY);
		if(blastSearchThreadsString != null) {
			blastSearchThreads = Integer.parseInt(blastSearchThreadsString);
		}
		return blastSearchThreads;
	}

	private String establishBlastType(BlastType blastType,
			PropertiesConfiguration propsConfig) {
		String blastExecutable;
		switch(blastType) {
		case BLASTN:
			blastExecutable = propsConfig.getPropertyValue(BLASTN_EXECUTABLE_PROPERTY);
			break;
		case TBLASTN:
			blastExecutable = propsConfig.getPropertyValue(TBLASTN_EXECUTABLE_PROPERTY);
			break;
		default:
			throw new BlastException(Code.UNKNOWN_BLAST_TYPE, blastType.name());
		}
		return blastExecutable;
	}

	
	protected String getBlastTempDir(CommandContext cmdContext) {
		String blastTempDir = cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(BLAST_TEMP_DIR_PROPERTY);
		if(blastTempDir == null) { throw new BlastException(Code.BLAST_CONFIG_EXCEPTION, "BLAST temp directory not defined in config property "+BLAST_TEMP_DIR_PROPERTY); }
		return blastTempDir;
	}

	

	private void addDoubleOption(Element configElem, String propertyName) {
		addDoubleOption(configElem, propertyName, null, false, null, false);
	}
	
	private void addDoubleOption(
			Element configElem, String propertyName, 
			Double minValue, boolean minInclusive,
			Double maxValue, boolean maxInclusive) {
		Double value = PluginUtils.configureDoubleProperty(configElem, propertyName, 
				minValue, minInclusive, 
				maxValue, maxInclusive,
				false);
		if(value != null) {
			numericCommandLineOptions.add(new Option<Double>(propertyName, value));
		}
	}

	private void addIntOption(Element configElem, String propertyName) {
		addIntOption(configElem, propertyName, null, false, null, false);
	}

	private void addIntOption(Element configElem, String propertyName, 
			Integer minValue, boolean minInclusive,
			Integer maxValue, boolean maxInclusive) {
		Integer value = PluginUtils.configureIntProperty(configElem, propertyName, false);
		if(value != null) {
			numericCommandLineOptions.add(new Option<Integer>(propertyName, value));
		}
	}

	private static class Option<C> {
		String name;
		C value;
		public Option(String name, C value) {
			super();
			this.name = name;
			this.value = value;
		}
		public String getName() {
			return name;
		}
		public C getValue() {
			return value;
		}
	}
	
}
