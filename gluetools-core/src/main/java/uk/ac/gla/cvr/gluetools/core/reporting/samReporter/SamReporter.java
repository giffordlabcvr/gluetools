package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Optional;
import java.util.logging.Level;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="samReporter")
public class SamReporter extends ModulePlugin<SamReporter> {

	public static final String ALIGNER_MODULE_NAME = "alignerModuleName";
	public static final String READ_LOG_INTERVAL = "readLogInterval";
	
	private String alignerModuleName;
	private Integer readLogInterval;
	
	public SamReporter() {
		super();
		addModulePluginCmdClass(SamVariationsScanCommand.class);
		addModulePluginCmdClass(SamNucleotidesCommand.class);
		addModulePluginCmdClass(SamAminoAcidCommand.class);
		addSimplePropertyName(ALIGNER_MODULE_NAME);
		addSimplePropertyName(READ_LOG_INTERVAL);
		
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignerModuleName = PluginUtils.configureStringProperty(configElem, ALIGNER_MODULE_NAME, false);
		this.readLogInterval = Optional.ofNullable(
				PluginUtils.configureIntProperty(configElem, READ_LOG_INTERVAL, false)).orElse(20000);
	}

	public String getAlignerModuleName() {
		return alignerModuleName;
	}

	public class RecordsCounter {
		int numRecords = 0;
		public void processedRecord() {
			numRecords++;
		}
		public void logRecordsProcessed() {
			if(numRecords % readLogInterval == 0) {
				log(Level.FINE, "Processed "+numRecords+" reads");
			}
		}
		public void logTotalRecordsProcessed() {
			log(Level.FINE, "Total reads processed: "+numRecords);
		}
	}


	
	
}
