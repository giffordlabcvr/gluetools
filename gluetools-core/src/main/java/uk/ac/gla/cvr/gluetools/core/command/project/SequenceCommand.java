package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceMode;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"sequence"}, 
	docoptUsages={"<sourceName> <sequenceID>", 
				"-w <whereClause>"},
	docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Specify based on field values"},
	description="Enter command mode to manage a sequence", 
	furtherHelp=
	"The optional whereClause allows a sequence to be specified via its field values.\n"+
	"If this query returns multiple or zero sequences, the command fails.\n"+
	"Examples:\n"+
	"  sequence -w \"gb_primary_accession = 'GR195721'\"\n"+
	"  sequence mySource 12823121")
@EnterModeCommandClass(
		commandFactoryClass = SequenceModeCommandFactory.class, 
		modeDescription = "Sequence mode")
public class SequenceCommand extends ProjectModeCommand<OkResult>  {

	public static final String SEQUENCE_ID = "sequenceID";
	public static final String SOURCE_NAME = "sourceName";
	private String sourceName;
	private String sequenceID;
	private Expression whereClause;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, false);
		sequenceID = PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, false);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false);
		
		if(whereClause == null) {
			if(sourceName == null || sequenceID == null) {
				usageError();
			}
		} else {
			if(sourceName != null || sequenceID != null) {
				usageError();
			}
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or both sourceName and sequenceID must be specified");
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Sequence sequence;
		if(whereClause == null) {
			sequence = GlueDataObject.lookup(cmdContext, Sequence.class, 
					Sequence.pkMap(sourceName, sequenceID));
		} else {
			SelectQuery selectQuery = new SelectQuery(Sequence.class, whereClause);
			List<Sequence> sequences = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
			int numSeqs = sequences.size();
			if(numSeqs == 1) {
				sequence = sequences.get(0);
			} else if(numSeqs == 0) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "Query returned no sequences.");
			} else {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "Query returned multiple sequences.");
			} 
		}
		Project project = getProjectMode(cmdContext).getProject();
		cmdContext.pushCommandMode(new SequenceMode(project, this, sequence.getSource().getName(), sequence.getSequenceID()));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
			registerVariableInstantiator("sequenceID", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return AdvancedCmdCompleter.listNames(cmdContext, prefix, Sequence.class, Sequence.SEQUENCE_ID_PROPERTY, 
							ExpressionFactory.matchExp(Sequence.SOURCE_NAME_PATH, bindings.get("sourceName")));
				}
			});
		}
	}

}
