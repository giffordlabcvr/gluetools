package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.digs.importer.model.Extracted;
import uk.ac.gla.cvr.gluetools.core.digs.importer.model.auto._Extracted;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"list", "extracted"}, 
		description = "Show some rows from a DIGS \"Extracted\" table", 
		docoptUsages = { "<digsDbName> [-w <whereClause>] [<fieldName> ...]" },
		docCategory = "Type-specific module commands",
		docoptOptions = {
				"-w <whereClause>, --whereClause <whereClause>  Qualify listed rows"
		},
		metaTags = {}, 
		furtherHelp = "Example:\n"+
		"  list extracted TD_Heterocephalus_RT -w \"scaffold = 'JH602050' and sequenceLength > 250\" blastId extractStart extractEnd" 
)
public class ListExtractedCommand extends DigsImporterCommand<ListExtractedResult> implements ProvidedProjectModeCommand {

	public static final String DIGS_DB_NAME = "digsDbName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String FIELD_NAME = "fieldName";
	
	private String digsDbName;
	private Optional<Expression> whereClause;
	private List<String> fieldNames;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.digsDbName = PluginUtils.configureStringProperty(configElem, DIGS_DB_NAME, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		fieldNames = PluginUtils.configureStringsProperty(configElem, FIELD_NAME);
		if(fieldNames.isEmpty()) {
			fieldNames = Arrays.asList(_Extracted.RECORD_ID_PROPERTY, _Extracted.BLAST_ID_PROPERTY, _Extracted.ORGANISM_PROPERTY, _Extracted.SCAFFOLD_PROPERTY, _Extracted.SEQUENCE_LENGTH_PROPERTY); // default fields
		}

	}

	@Override
	protected ListExtractedResult execute(CommandContext cmdContext, DigsImporter digsImporter) {
		return digsImporter.listExtracted(cmdContext, digsDbName, whereClause, fieldNames);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("digsDbName", new DigsDbNameInstantiator());
			registerVariableInstantiator("fieldName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					return Arrays.asList(Extracted.ALL_PROPERTIES).stream()
							.map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
					}
			});
		}
		
	}
	
}
