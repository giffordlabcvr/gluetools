package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"populate"}, 
		docoptUsages={"[-b <batchSize>] [-w <whereClause>] [-p] -f <fileName> [<fieldName> ...]"},
		docoptOptions={
				"-b <batchSize>, --batchSize <batchSize>        Commit batch size [default: 250]",
				"-p, --preview                                  Preview only, no DB updates",
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated sequences",
				"-f <fileName>, --fileName <fileName>           Text file with field values"
		},
		description="Populate sequence field values based on a text file", 
		metaTags = { CmdMeta.consoleOnly , CmdMeta.updatesDatabase},
		furtherHelp="The file is loaded from a location relative to the current load/save directory."+
		"The <batchSize> argument allows you to control how often updates are committed to the database "+
				"during the import. The default is every 250 text file lines. A larger <batchSize> means fewer database "+
				"accesses, but requires more Java heap memory."+
				"If <fieldName> arguments are supplied, the populator will not update any field unless it appears in the <fieldName> list. "+
				"If no <fieldName> arguments are supplied, the populator may update any field.") 
public class TextFilePopulatorPopulateCommand extends ModulePluginCommand<TextFilePopulatorResult, TextFilePopulator> implements ProvidedProjectModeCommand {

	public static final String WHERE_CLAUSE = "whereClause";
	public static final String FIELD_NAME = "fieldName";


	private Integer batchSize;
	private Boolean preview;
	private String fileName;
	private List<String> fieldNames;
	private Optional<Expression> whereClause;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "batchSize", false)).orElse(250);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
		preview = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "preview", false)).orElse(false);
		fieldNames = PluginUtils.configureStringsProperty(configElem, FIELD_NAME);
		if(fieldNames.isEmpty()) {
			fieldNames = null; // default fields
		}
	}
	
	@Override
	protected TextFilePopulatorResult execute(CommandContext cmdContext, TextFilePopulator populatorPlugin) {
		return new TextFilePopulatorResult(populatorPlugin.populate((ConsoleCommandContext) cmdContext, fileName, batchSize, whereClause, preview, fieldNames));
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
			registerVariableInstantiator("fieldName", new ModifiableFieldNameInstantiator(ConfigurableTable.sequence.name()));
		}
	}

	
}