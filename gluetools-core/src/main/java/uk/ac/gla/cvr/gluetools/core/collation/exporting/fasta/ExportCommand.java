package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@CommandClass( 
		commandWords={"export"}, 
		docoptUsages={"(-w <whereClause> | -a) [-y <lineFeedStyle>] -f <fileName>"},
		docoptOptions={
				"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>  LF or CRLF",
				"-f <fileName>, --fileName <fileName>                 FASTA file",
				"-w <whereClause>, --whereClause <whereClause>        Qualify exported sequences",
			    "-a, --allSequences                                   Export all project sequences"},
		metaTags = { CmdMeta.consoleOnly },
		description="Export sequences to a FASTA file", 
		furtherHelp="The file is saved to a location relative to the current load/save directory.") 
public class ExportCommand extends BaseExportCommand<OkResult> implements ProvidedProjectModeCommand {

	private String fileName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
	}

	@Override
	protected OkResult execute(CommandContext cmdContext, FastaExporter importerPlugin) {
		byte[] fastaBytes = importerPlugin.doExport(cmdContext, getWhereClause(), getLineFeedStyle());
		((ConsoleCommandContext) cmdContext).saveBytes(fileName, fastaBytes);
		return new OkResult();

	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerEnumLookup("lineFeedStyle", LineFeedStyle.class);
			registerPathLookup("fileName", false);
		}
	}

}