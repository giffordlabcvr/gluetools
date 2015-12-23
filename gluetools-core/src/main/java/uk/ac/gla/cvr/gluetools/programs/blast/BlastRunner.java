package uk.ac.gla.cvr.gluetools.programs.blast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAlignerException;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAlignerException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResultBuilder.BlastXPath;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.SingleReferenceBlastDB;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;

public class BlastRunner implements Plugin {

	public static String 
		BLASTN_EXECUTABLE_PROPERTY = "gluetools.core.programs.blast.blastn.executable"; 


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
	public List<BlastResult> executeBlast(CommandContext cmdContext, String refName, byte[] fastaBytes) {
		SingleReferenceBlastDB refDB = BlastDbManager.getInstance().ensureSingleReferenceDB(cmdContext, refName);
		refDB.readLock().lock();
		ProcessResult blastProcessResult;
		try {
			String blastNexecutable = 
					cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getPropertyValue(BLASTN_EXECUTABLE_PROPERTY);
			List<String> commandWords = new ArrayList<String>();
			commandWords.add(blastNexecutable);
			// supply reference DB
			commandWords.add("-db");
			commandWords.add(new File(refDB.getBlastDbDir(cmdContext), BlastDbManager.BLAST_DB_PREFIX).getAbsolutePath());
			// outfmt 14 is XML2
			commandWords.add("-outfmt");
			commandWords.add("14");
			for(Option numericOption : numericCommandLineOptions) {
				commandWords.add("-"+numericOption.getName());
				commandWords.add(numericOption.getValue().toString());
			}
			// run blast based on the ref DB.
			blastProcessResult = ProcessUtils.runProcess(new ByteArrayInputStream(fastaBytes), commandWords); 
		} finally {
			refDB.readLock().unlock();
		}
		List<Document> resultDocs;
		try {
			resultDocs = GlueXmlUtils.documentsFromBytes(blastProcessResult.getOutputBytes());
		} catch(Exception e) {
			throw new BlastAlignerException(Code.BLAST_OUTPUT_FORMAT_ERROR, e.getLocalizedMessage());
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
