package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="fastaExporter")
public class FastaExporter extends AbstractFastaExporter<FastaExporter> {


	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		addProvidedCmdClass(ExportCommand.class);
		addProvidedCmdClass(ShowExporterCommand.class);
		addProvidedCmdClass(ConfigureExporterCommand.class);
	}

	public OkResult doExport(ConsoleCommandContext cmdContext, String fileName, Expression whereClause) {
		
		SelectQuery selectQuery = null;
		if(whereClause != null) {
			selectQuery = new SelectQuery(Sequence.class, whereClause);
		} else {
			selectQuery = new SelectQuery(Sequence.class);
		}
		List<Sequence> sequences = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
		StringBuffer stringBuffer = new StringBuffer();
		sequences.forEach(seq -> {
			String fastaId = generateFastaId(seq);
			stringBuffer.append(FastaUtils.seqIdCompoundsPairToFasta(fastaId, seq.getSequenceObject().getNucleotides(cmdContext)));
		});
		cmdContext.saveBytes(fileName, stringBuffer.toString().getBytes());
		return new OkResult();
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
	public static class ExportCommand extends ModuleProvidedCommand<OkResult, FastaExporter> implements ProvidedProjectModeCommand {

		private String fileName;
		private Expression whereClause;
		private Boolean allSequences;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
			whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false);
			allSequences = PluginUtils.configureBooleanProperty(configElem, "allSequences", true);
			if(whereClause == null && !allSequences) {
				usageError();
			}
			if(whereClause != null && allSequences) {
				usageError();
			}
		}

		private void usageError() {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "Either <whereClause> or <allSequences> must be specified, but not both");
		}
		
		@Override
		protected OkResult execute(CommandContext cmdContext, FastaExporter importerPlugin) {
			return importerPlugin.doExport((ConsoleCommandContext) cmdContext, fileName, whereClause);
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
			commandWords={"show", "configuration"}, 
			docoptUsages={},
			description="Show the current configuration of this exporter") 
	public static class ShowExporterCommand extends ShowConfigCommand<FastaExporter> {}

	@SimpleConfigureCommandClass(
			propertyNames={"idTemplate"}
	)
	public static class ConfigureExporterCommand extends SimpleConfigureCommand<FastaExporter> {}


}
