package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete","sequence"}, 
	docoptUsages={"<sourceName> <sequenceID>", 
			"-w <whereClause>",
			"-a"},
	metaTags={CmdMeta.updatesDatabase},
	docoptOptions={"-w <whereClause>, --whereClause <whereClause>  Qualify which sequences should be deleted", 
			"-a, --allSequences  Delete all sequences" },
	description="Delete one or more sequences", 
	furtherHelp="If <allSequences> or <whereClause> is used, there will be no attempt to delete reference sequences.") 
public class DeleteSequenceCommand extends ProjectModeCommand<DeleteResult> {

	private Optional<String> sourceName;
	private Optional<String> sequenceID;
	private Boolean allSequences;
	private Optional<Expression> whereClause;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "sourceName", false));
		sequenceID = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "sequenceID", false));
		allSequences = PluginUtils.configureBooleanProperty(configElem, "allSequences", true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false));
		if( !(
				(sourceName.isPresent() && sequenceID.isPresent() && !allSequences && !whereClause.isPresent()) || 
				(!sourceName.isPresent() && !sequenceID.isPresent() && allSequences && !whereClause.isPresent()) || 
				(!sourceName.isPresent() && !sequenceID.isPresent() && !allSequences && whereClause.isPresent()) 
				)) {
			usageError();
		}
		
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or allSequences or both sourceName and sequenceID must be specified");
	}
	
	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		if(sourceName.isPresent()) {
			DeleteResult result = GlueDataObject.delete(cmdContext.getObjectContext(), 
					Sequence.class, Sequence.pkMap(sourceName.get(), sequenceID.get()), true);
			cmdContext.commit();
			return result; 
		} else {
			SelectQuery selectQuery = null;
			if(whereClause.isPresent()) {
				selectQuery = new SelectQuery(Sequence.class, whereClause.get());
			} else {
				selectQuery = new SelectQuery(Sequence.class);
			}
			List<Sequence> sequencesToDelete = 
					GlueDataObject.query(cmdContext.getObjectContext(), Sequence.class, selectQuery);
			// filter out reference sequences
			sequencesToDelete = sequencesToDelete.stream()
					.filter(seq -> seq.getReferenceSequences().isEmpty())
					.collect(Collectors.toList());
			int numDeleted = 0;
			for(Sequence seqToDelete: sequencesToDelete) {
				DeleteResult result = GlueDataObject.delete(cmdContext.getObjectContext(), Sequence.class, seqToDelete.pkMap(), true);
				numDeleted = numDeleted+result.getNumber();
			}
			cmdContext.commit();
			return new DeleteResult(Sequence.class, numDeleted);
		}
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
			registerVariableInstantiator("sequenceID", 
					new QualifiedDataObjectNameInstantiator(Sequence.class, Sequence.SEQUENCE_ID_PROPERTY) {
				@Override
				@SuppressWarnings("rawtypes")
				protected void qualifyResults(CommandMode cmdMode,
						Map<String, Object> bindings, Map<String, Object> qualifierValues) {
					qualifierValues.put(Sequence.SOURCE_NAME_PATH, bindings.get("sourceName"));
				}
			});
		}
	}

}
