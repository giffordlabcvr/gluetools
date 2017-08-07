package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier.AbstractSequenceSupplier;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier.MemberQuerySequenceSupplier;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@CommandClass( 
		commandWords={"export-member"}, 
		docoptUsages={"<alignmentName> [-c] [-w <whereClause>] [-y <lineFeedStyle>] (-p | -f <fileName>)"},
		docoptOptions={
				"-c, --recursive                                      Include members of descendent alignments",
				"-w <whereClause>, --whereClause <whereClause>        Qualify exported members",
				"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>  LF or CRLF",
				"-f <fileName>, --fileName <fileName>                 FASTA file",
				"-p, --preview                                        Preview output"
},
		metaTags = { CmdMeta.consoleOnly },
		description="Export the sequences of alignment members to a FASTA file", 
		furtherHelp="The file is saved to a location relative to the current load/save directory.") 
public class ExportMemberCommand extends BaseExportMemberCommand<CommandResult> implements ProvidedProjectModeCommand {

	public static final String PREVIEW = "preview";
	public static final String FILE_NAME = "fileName";
	
	private String fileName;
	private boolean preview;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, true);
		if(fileName == null && !preview || fileName != null && preview) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <fileName> or <preview> must be specified, but not both");
		}
	}

	@Override
	protected CommandResult execute(CommandContext cmdContext, FastaExporter fastaExporter) {
		
		AbstractSequenceSupplier sequenceSupplier = 
				new MemberQuerySequenceSupplier(getAlignmentName(), getRecursive(), Optional.ofNullable(getWhereClause()));

		if(preview) {
			ByteArrayOutputStream previewBaos = new ByteArrayOutputStream();
			PrintWriter printWriter = new PrintWriter(previewBaos);
			super.export(cmdContext, sequenceSupplier, fastaExporter, printWriter);
			return new SimpleConsoleCommandResult(new String(previewBaos.toByteArray()));
		} else {
			ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
			try(OutputStream outputStream = consoleCmdContext.openFile(fileName)) {
				PrintWriter printWriter = new PrintWriter(new BufferedOutputStream(outputStream, 65536));
				super.export(cmdContext, sequenceSupplier, fastaExporter, printWriter);
			} catch (IOException ioe) {
				throw new CommandException(ioe, Code.COMMAND_FAILED_ERROR, "Failed to write alignment file: "+ioe.getMessage());
			}
			return new OkResult();
		}
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerEnumLookup("lineFeedStyle", LineFeedStyle.class);
			registerPathLookup("fileName", false);
		}
	}

}