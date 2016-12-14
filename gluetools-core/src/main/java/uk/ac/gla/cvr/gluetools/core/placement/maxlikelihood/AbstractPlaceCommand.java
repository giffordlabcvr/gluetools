package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

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

	
	protected String getDataDir() {
		return dataDir;
	}


	protected String getOutputFile() {
		return outputFile;
	}


	protected static class AbstractPlaceCommandCompleter extends AdvancedCmdCompleter {
		public AbstractPlaceCommandCompleter() {
			super();
			registerPathLookup("dataDir", true);
			registerPathLookup("outputFile", false);
		}
	}

}
