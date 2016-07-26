package uk.ac.gla.cvr.gluetools.programs.blast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastException.Code;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResultBuilder.BlastXPath;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDB;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
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

	

	@SuppressWarnings("rawtypes")
	public List<BlastResult> executeBlast(CommandContext cmdContext, BlastType blastType, BlastDB blastDB, byte[] fastaBytes) {
		blastDB.readLock().lock();
		ProcessResult blastProcessResult;
		try {
			String blastExecutable;
			PropertiesConfiguration propsConfig = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
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
			
			Integer blastSearchThreads = null;
			String blastSearchThreadsString = propsConfig.getPropertyValue(BLAST_SEARCH_THREADS_PROPERTY);
			if(blastSearchThreadsString != null) {
				blastSearchThreads = Integer.parseInt(blastSearchThreadsString);
			}
			
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
			// run blast based on the ref DB.
			blastProcessResult = ProcessUtils.runProcess(new ByteArrayInputStream(fastaBytes), null, commandWords); 
		} finally {
			blastDB.readLock().unlock();
		}
		List<Document> resultDocs;
		try {
			resultDocs = GlueXmlUtils.documentsFromBytes(blastProcessResult.getOutputBytes());
		} catch(Exception e) {
			GlueLogger.log(Level.FINE, "BLAST stderr:\n"+new String(blastProcessResult.getErrorBytes()));
			throw new BlastException(BlastException.Code.BLAST_OUTPUT_FORMAT_ERROR, e.getLocalizedMessage());
		}
		// use same xPathEngine across results, should save some init time.
		// guess a faster way may be to use a SAX parser.
		BlastXPath blastXPath = new BlastXPath();
		List<BlastResult> blastResults = new ArrayList<BlastResult>();
		for(Document resultDoc: resultDocs) {
			blastResults.add(BlastResultBuilder.blastResultFromDocument(blastXPath, resultDoc));
		}
		return blastResults;
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
