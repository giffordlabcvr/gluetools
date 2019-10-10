/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.fastaExporter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.BaseExportSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier.AbstractSequenceSupplier;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.sequenceSupplier.QuerySequenceSupplier;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.NucleotideFastaCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@CommandClass( 
		commandWords={"export"}, 
		docoptUsages={"(-w <whereClause> | -a) [-y <lineFeedStyle>] (-p | -f <fileName>)"},
		docoptOptions={
				"-y <lineFeedStyle>, --lineFeedStyle <lineFeedStyle>  LF or CRLF",
				"-f <fileName>, --fileName <fileName>                 FASTA file",
				"-w <whereClause>, --whereClause <whereClause>        Qualify exported sequences",
			    "-a, --allSequences                                   Export all project sequences",
				"-p, --preview                                        Preview output"},
		metaTags = { CmdMeta.consoleOnly },
		description="Export sequences to a FASTA file", 
		furtherHelp="The file is saved to a location relative to the current load/save directory.") 
public class ExportCommand extends BaseExportSequenceCommand<CommandResult> implements ProvidedProjectModeCommand {

	public static final String PREVIEW = "preview";
	public static final String FILE_NAME = "fileName";

	private Boolean preview;
	private String fileName;

	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, false);
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, true);
		if(fileName == null && !preview || fileName != null && preview) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <fileName> or <preview> must be specified, but not both");
		}
	}
	
	@Override
	protected CommandResult execute(CommandContext cmdContext, FastaExporter fastaExporter) {
		
		AbstractSequenceSupplier sequenceSupplier = 
				new QuerySequenceSupplier(Optional.ofNullable(getWhereClause()));

		if(preview) {
			Map<String, DNASequence> ntFastaMap = super.export(cmdContext, sequenceSupplier, fastaExporter);
			return new NucleotideFastaCommandResult(ntFastaMap);
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
			registerEnumLookup("lineFeedStyle", LineFeedStyle.class);
			registerPathLookup("fileName", false);
		}
	}

}
