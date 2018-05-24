package uk.ac.gla.cvr.gluetools.core.samFileGenerator;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

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
		try(OutputStream outputStream = consoleCmdContext.openFile(fileName)) {
			SAMFileWriter samFileWriter = createSAMFileWriter(samFileHeader, outputStream);
			samFileGenerator.writeReads(cmdContext, samFileHeader, samFileWriter);
		} catch (IOException e) {
			throw new ConsoleException(e, Code.WRITE_ERROR, fileName, e.getMessage());
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
