package uk.ac.gla.cvr.gluetools.core.reporting.figtree.annotationExporter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"export", "figtree-annotation"}, 
		description = "Export a figtree annotation file based on alignment members", 
		docoptUsages = { "<almtName> [-c] [-w <whereClause>] -f <fileName>"},
		docoptOptions = { 
				"-c, --recursive                                Include descendent members",
				"-w <whereClause>, --whereClause <whereClause>  Qualify members",
				"-f <fileName>, --fileName <fileName>           Output to file",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class ExportAnnotationsCommand extends ModulePluginCommand<CommandResult, FigTreeAnnotationExporter>{

	public static final String ALIGNMENT_NAME = "almtName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String FILE_NAME = "fileName";
	
	private String almtName;
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.almtName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.recursive = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, RECURSIVE, false)).orElse(false);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}

	@Override
	protected CommandResult execute(CommandContext cmdContext, FigTreeAnnotationExporter figTreeAnnotationExporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;

		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName));
		StringBuffer buf = new StringBuffer();
		List<String> columnHeaders = figTreeAnnotationExporter.getColumnHeaders(cmdContext);
		buf.append(String.join("\t", columnHeaders)).append("\n");
		List<List<String>> annotationRows = figTreeAnnotationExporter.getAnnotationRows(consoleCmdContext, alignment, whereClause, recursive);
		for(List<String> annotationRow: annotationRows) {
			buf.append(String.join("\t", annotationRow)).append("\n");
		}
		consoleCmdContext.saveBytes(fileName, buf.toString().getBytes());
		return new OkResult();
		
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			registerDataObjectNameLookup("almtName", Alignment.class, Alignment.NAME_PROPERTY);
			registerPathLookup("fileName", false);
		}
	}
	
}
