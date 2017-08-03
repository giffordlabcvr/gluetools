package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandBinaryResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@CommandClass( 
		commandWords={"web-export"}, 
		docoptUsages={"(-w <whereClause> | -a)"},
		docoptOptions={
			"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>  LF or CRLF",
			"-w <whereClause>, --whereClause <whereClause>        Qualify exported sequences",
		    "-a, --allSequences                                   Export all project sequences"},
		metaTags = { CmdMeta.webApiOnly, CmdMeta.producesBinary },
		description="Export sequences to a FASTA file") 
public class WebExportCommand extends BaseExportCommand<CommandBinaryResult> implements ProvidedProjectModeCommand {

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
	}

	public CommandBinaryResult execute(CommandContext cmdContext, FastaExporter importerPlugin) {
		byte[] fastaBytes = importerPlugin.doExport(cmdContext, getWhereClause(), getLineFeedStyle());
		return new CommandBinaryResult("fastaExportResult", fastaBytes);
	}
	
}