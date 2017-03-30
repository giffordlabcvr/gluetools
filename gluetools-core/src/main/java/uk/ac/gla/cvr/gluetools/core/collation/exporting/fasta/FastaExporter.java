package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandBinaryResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.GenbankXmlSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="fastaExporter")
public class FastaExporter extends AbstractFastaExporter<FastaExporter> {


	public FastaExporter() {
		super();
		addModulePluginCmdClass(ExportCommand.class);
		addModulePluginCmdClass(WebExportCommand.class);
	}

	public byte[] doExport(CommandContext cmdContext, Expression whereClause) {
		
		long startTime = System.currentTimeMillis();
		GenbankXmlSequenceObject.msInXPath = 0;
		GenbankXmlSequenceObject.msInDocParsing = 0;
		
		SelectQuery selectQuery = null;
		if(whereClause != null) {
			selectQuery = new SelectQuery(Sequence.class, whereClause);
		} else {
			selectQuery = new SelectQuery(Sequence.class);
		}
		int totalNumSeqs = GlueDataObject.count(cmdContext, selectQuery);
		int batchSize = 500;
		int offset = 0;
		selectQuery.setFetchLimit(batchSize);
		selectQuery.setPageSize(batchSize);
		StringBuffer stringBuffer = new StringBuffer();

		while(offset < totalNumSeqs) {
			selectQuery.setFetchOffset(offset);
			int lastBatchIndex = Math.min(offset+batchSize, totalNumSeqs);
			GlueLogger.getGlueLogger().info("Retrieving sequences "+(offset+1)+" to "+lastBatchIndex+" of "+totalNumSeqs);
			List<Sequence> sequences = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
			GlueLogger.getGlueLogger().info("Processing sequences "+(offset+1)+" to "+lastBatchIndex+" of "+totalNumSeqs);
			sequences.forEach(seq -> {
				String fastaId = generateFastaId(seq);
				stringBuffer.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId, seq.getSequenceObject().getNucleotides(cmdContext)));
			});
			offset += batchSize;
		}
		GlueLogger.getGlueLogger().info("Time for doExport was "+(System.currentTimeMillis() - startTime)+"ms");
		GlueLogger.getGlueLogger().info("Time in genbank sequence xpath was "+GenbankXmlSequenceObject.msInXPath+"ms");
		GlueLogger.getGlueLogger().info("Time in genbank document parsing was "+GenbankXmlSequenceObject.msInDocParsing+"ms");

		
		return stringBuffer.toString().getBytes();
	}

	private static abstract class BaseExportCommand<R extends CommandResult> extends ModulePluginCommand<R, FastaExporter> implements ProvidedProjectModeCommand {
		
		private Expression whereClause;
		private Boolean allSequences;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false);
			allSequences = PluginUtils.configureBooleanProperty(configElem, "allSequences", true);
			if(whereClause == null && !allSequences) {
				usageError();
			}
			if(whereClause != null && allSequences) {
				usageError();
			}
		}

		protected Expression getWhereClause() {
			return whereClause;
		}

		private void usageError() {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allSequences> must be specified, but not both");
		}

	}
	
	
	@CommandClass( 
			commandWords={"export"}, 
			docoptUsages={"(-w <whereClause> | -a) -f <fileName>"},
			docoptOptions={
				"-f <fileName>, --fileName <fileName>           FASTA file",
				"-w <whereClause>, --whereClause <whereClause>  Qualify exported sequences",
			    "-a, --allSequences                             Export all project sequences"},
			metaTags = { CmdMeta.consoleOnly },
			description="Export sequences to a FASTA file", 
			furtherHelp="The file is saved to a location relative to the current load/save directory.") 
	public static class ExportCommand extends BaseExportCommand<OkResult> implements ProvidedProjectModeCommand {

		private String fileName;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
		}

		@Override
		protected OkResult execute(CommandContext cmdContext, FastaExporter importerPlugin) {
			byte[] fastaBytes = importerPlugin.doExport(cmdContext, getWhereClause());
			((ConsoleCommandContext) cmdContext).saveBytes(fileName, fastaBytes);
			return new OkResult();

		}
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerPathLookup("fileName", false);
			}
		}

	}

	
	
	@CommandClass( 
			commandWords={"web-export"}, 
			docoptUsages={"(-w <whereClause> | -a)"},
			docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify exported sequences",
			    "-a, --allSequences                             Export all project sequences"},
			metaTags = { CmdMeta.webApiOnly, CmdMeta.producesBinary },
			description="Export sequences to a FASTA file", 
			furtherHelp="The file is saved to a location relative to the current load/save directory.") 
	public static class WebExportCommand extends BaseExportCommand<CommandBinaryResult> implements ProvidedProjectModeCommand {

		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
		}

		public CommandBinaryResult execute(CommandContext cmdContext, FastaExporter importerPlugin) {
			byte[] fastaBytes = importerPlugin.doExport(cmdContext, getWhereClause());
			return new CommandBinaryResult("fastaExportResult", fastaBytes);
		}
		
	}

	
	
	
}
