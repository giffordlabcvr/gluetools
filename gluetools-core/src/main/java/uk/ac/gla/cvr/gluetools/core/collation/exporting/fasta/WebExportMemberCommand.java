package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandBinaryResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@CommandClass( 
		commandWords={"web-export-member"}, 
		docoptUsages={"<alignmentName> [-c] [-w <whereClause>] [-y <lineFeedStyle>]"},
		docoptOptions={
				"-c, --recursive                                      Include members of descendent alignments",
				"-w <whereClause>, --whereClause <whereClause>        Qualify exported members",
				"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>  LF or CRLF"
		},
		metaTags = { CmdMeta.webApiOnly, CmdMeta.producesBinary },
		description="Export the sequences of alignment members to a FASTA file") 
public class WebExportMemberCommand extends BaseExportMemberCommand<CommandBinaryResult> implements ProvidedProjectModeCommand {

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
	}

	public CommandBinaryResult execute(CommandContext cmdContext, FastaExporter importerPlugin) {
		byte[] fastaBytes = importerPlugin.doExportMembers(cmdContext, getAlignmentName(), getRecursive(), getWhereClause(), getLineFeedStyle());
		return new CommandBinaryResult("fastaExportResult", fastaBytes);
	}
	
}