package uk.ac.gla.cvr.gluetools.core.samFileGenerator;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;

import java.io.OutputStream;
import java.util.logging.Level;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.samFileGenerator.SamFileGeneratorException.Code;

public abstract class AbstractGenerateFileCommand extends ModulePluginCommand<OkResult, SamFileGenerator>{

	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
	}

	@Override
	protected OkResult execute(CommandContext cmdContext, SamFileGenerator samFileGenerator) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		SAMFileHeader samFileHeader = samFileGenerator.generateHeader(cmdContext);
		SAMFileWriter samFileWriter = null;
		try(OutputStream outputStream = consoleCmdContext.openFile(fileName)) {
			samFileWriter = createSAMFileWriter(samFileHeader, outputStream);
			samFileGenerator.writeReads(cmdContext, samFileHeader, samFileWriter);
			if(samFileWriter != null) {
				try {
					samFileWriter.close();
				} catch(Exception e) {
					GlueLogger.log(Level.WARNING, "Failed to close samFileWriter: "+e.getLocalizedMessage());
				}
			}
		} catch(Exception e) {
			throw new SamFileGeneratorException(e, Code.IO_ERROR, 
					"Error writing SAM/BAM file "+fileName+": "+e.getLocalizedMessage());
		}
		return new OkResult();
	}
	
	public static class GenerateFileCommandCompleter extends AdvancedCmdCompleter {
		public GenerateFileCommandCompleter() {
			super();
			registerPathLookup("fileName", false);
		}
	}

	protected abstract SAMFileWriter createSAMFileWriter(SAMFileHeader header, OutputStream outputStream);

}
