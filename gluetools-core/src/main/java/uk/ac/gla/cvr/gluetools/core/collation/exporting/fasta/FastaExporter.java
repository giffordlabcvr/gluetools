package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

@PluginClass(elemName="fastaExporter")
public class FastaExporter extends ModulePlugin<FastaExporter> {

	private Template idTemplate;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		idTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, "idTemplate", false);
		addProvidedCmdClass(ExportCommand.class);
		addProvidedCmdClass(ShowExporterCommand.class);
		addProvidedCmdClass(ConfigureExporterCommand.class);
	}

	public OkResult doExport(ConsoleCommandContext cmdContext, String fileName, Expression whereClause) {
		ObjectContext objContext = cmdContext.getObjectContext();
		SelectQuery selectQuery = null;
		if(whereClause != null) {
			selectQuery = new SelectQuery(Sequence.class, whereClause);
		} else {
			selectQuery = new SelectQuery(Sequence.class);
		}
		List<Sequence> sequences = GlueDataObject.query(objContext, Sequence.class, selectQuery);
		StringBuffer stringBuffer = new StringBuffer();
		sequences.forEach(seq -> {
			String fastaId;
			if(idTemplate == null) {
				fastaId = seq.getSequenceID();
			} else {
				TemplateHashModel variableResolver = new TemplateHashModel() {
					@Override
					public TemplateModel get(String key) {
						return new SimpleScalar(seq.readNestedProperty(key).toString()); 
					}
					@Override
					public boolean isEmpty() { return false; }
				};
				StringWriter result = new StringWriter();
				try {
					idTemplate.process(variableResolver, result);
				} catch (TemplateException e) {
					throw new CommandException(e, Code.COMMAND_FAILED_ERROR, e.getLocalizedMessage());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				fastaId = result.toString();
			}
			stringBuffer.append(FastaUtils.seqIdNtsPairToFasta(fastaId, seq.getSequenceObject().getNucleotides()));
		});
		cmdContext.saveBytes(fileName, stringBuffer.toString().getBytes());
		return new OkResult();
	}

	
	
	@CommandClass( 
			commandWords={"export"}, 
			docoptUsages={"(-w <whereClause> | -a) -f <file>"},
			docoptOptions={
				"-f <file>, --fileName <file>                   FASTA file",
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
