package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentUtils;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class AbstractPlacerResultCommand<R extends CommandResult> extends ModulePluginCommand<R, MaxLikelihoodPlacer> {

	public final static String INPUT_FILE = "inputFile";

	private String inputFile;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.inputFile = PluginUtils.configureStringProperty(configElem, INPUT_FILE, true);
	}

	@Override
	protected final R execute(CommandContext cmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer) {
		ConsoleCommandContext consoleCmdContext = ((ConsoleCommandContext) cmdContext);
		MaxLikelihoodPlacerResult placerResult = loadPlacerResult(consoleCmdContext, inputFile);
		return executeOnPlacerResult(cmdContext, maxLikelihoodPlacer, placerResult);
	}

	protected abstract R executeOnPlacerResult(CommandContext cmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer, 
			MaxLikelihoodPlacerResult placerResult);

	protected static MaxLikelihoodPlacerResult loadPlacerResult(ConsoleCommandContext consoleCmdContext, String inputFile) {
		byte[] placerResultBytes = consoleCmdContext.loadBytes(inputFile);
		Document placerResultDocument = GlueXmlUtils.documentFromBytes(placerResultBytes);
		CommandDocument placerResultCmdDoc = CommandDocumentXmlUtils.xmlDocumentToCommandDocument(placerResultDocument);
		MaxLikelihoodPlacerResult placerResult = PojoDocumentUtils.commandObjectToPojo(placerResultCmdDoc, MaxLikelihoodPlacerResult.class);
		return placerResult;
	}

	protected static class AbstractPlacerResultCommandCompleter extends AdvancedCmdCompleter {
		public AbstractPlacerResultCommandCompleter() {
			super();
			registerPathLookup("inputFile", false);
		}
	}

}
