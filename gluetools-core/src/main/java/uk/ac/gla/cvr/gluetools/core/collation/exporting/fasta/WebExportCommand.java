package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier.AbstractSequenceSupplier;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier.QuerySequenceSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandWebFileResult;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManager;

@CommandClass( 
		commandWords={"web-export"}, 
		docoptUsages={"(-w <whereClause> | -a) -f <fileName>"},
		docoptOptions={
			"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>  LF or CRLF",
			"-w <whereClause>, --whereClause <whereClause>        Qualify exported sequences",
		    "-a, --allSequences                                   Export all project sequences",
			"-f <fileName>, --fileName <fileName>                 FASTA output file"},
		metaTags = { CmdMeta.webApiOnly },
		description="Export sequences to a FASTA file") 
public class WebExportCommand extends BaseExportSequenceCommand<CommandWebFileResult> implements ProvidedProjectModeCommand {

	public static final String FILE_NAME = "fileName";
	
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}

	public CommandWebFileResult execute(CommandContext cmdContext, FastaExporter fastaExporter) {
		WebFilesManager webFilesManager = cmdContext.getGluetoolsEngine().getWebFilesManager();
		String subDirUuid = webFilesManager.createSubDir();
		webFilesManager.createWebFileResource(subDirUuid, fileName);

		AbstractSequenceSupplier sequenceSupplier = 
				new QuerySequenceSupplier(Optional.ofNullable(getWhereClause()));
		try(OutputStream outputStream = webFilesManager.appendToWebFileResource(subDirUuid, fileName)) {
			PrintWriter printWriter = new PrintWriter(new BufferedOutputStream(outputStream, 65536));
			super.export(cmdContext, sequenceSupplier, fastaExporter, printWriter);
		} catch(Exception e) {
			throw new CommandException(e, Code.COMMAND_FAILED_ERROR, "Write to web file resource "+subDirUuid+"/"+fileName+" failed: "+e.getMessage());
		}
		String webFileSizeString = webFilesManager.getSizeString(subDirUuid, fileName);
		
		return new CommandWebFileResult("fastaWebExportResult", subDirUuid, fileName, webFileSizeString);
	}
	
}
