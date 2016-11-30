package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.io.File;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentUtils;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class AbstractPlaceCommand<R extends CommandResult> extends ModulePluginCommand<R, MaxLikelihoodPlacer> {

	public final static String DATA_DIR = "dataDir";
	public final static String OUTPUT_FILE = "outputFile";

	private String dataDir;
	private String outputFile;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.dataDir = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);
		this.outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
	}

	
	protected void runPlacer(ConsoleCommandContext consoleCmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer,
			Map<String, DNASequence> querySequenceMap) {
		File dataDirFile = CommandUtils.ensureDataDir(consoleCmdContext, dataDir);
		MaxLikelihoodPlacerResult placerResult = maxLikelihoodPlacer.place(consoleCmdContext, querySequenceMap, dataDirFile);
		CommandDocument placerResultCmdDocument = PojoDocumentUtils.pojoToCommandDocument(placerResult);
		Document placerResultXmlDoc = CommandDocumentXmlUtils.commandDocumentToXmlDocument(placerResultCmdDocument);
		byte[] placerResultXmlBytes = GlueXmlUtils.prettyPrint(placerResultXmlDoc);
		consoleCmdContext.saveBytes(outputFile, placerResultXmlBytes);
	}


	
	protected static class AbstractPlaceCommandCompleter extends AdvancedCmdCompleter {
		public AbstractPlaceCommandCompleter() {
			super();
			registerPathLookup("dataDir", true);
			registerPathLookup("outputFile", false);
		}
	}

}
