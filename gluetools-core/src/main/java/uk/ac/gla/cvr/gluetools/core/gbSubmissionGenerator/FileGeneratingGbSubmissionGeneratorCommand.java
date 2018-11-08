package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator;

import java.io.File;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class FileGeneratingGbSubmissionGeneratorCommand<SR, R extends CommandResult> extends BaseGbSubmissionGeneratorCommand<File, SR, R> {

	public final static String OUTPUT_DIR = "outputDir";
	private String outputDir;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.outputDir = PluginUtils.configureStringProperty(configElem, OUTPUT_DIR, false);
	}
	
	@Override
	protected File initContext(ConsoleCommandContext consoleCmdContext) {
		File outputDirFile;
		
		if(outputDir == null) {
			outputDirFile = consoleCmdContext.getLoadSavePath();
		} else {
			outputDirFile = consoleCmdContext.fileStringToFile(outputDir);
			outputDirFile.mkdirs();
			if(!outputDirFile.exists()) {
				throw new Tbl2AsnException(Tbl2AsnException.Code.TBL2ASN_FILE_EXCEPTION, 
						"Unable to create output directory "+outputDirFile.getAbsolutePath());

			}
		}		
		return outputDirFile;
	}
	
	public static class FileGeneratingGbSubmissionGeneratorCompleter extends GbSubmissionGeneratorCompleter {
		public FileGeneratingGbSubmissionGeneratorCompleter() {
			super();
			registerPathLookup("outputDir", true);
		}
	}

	
}
