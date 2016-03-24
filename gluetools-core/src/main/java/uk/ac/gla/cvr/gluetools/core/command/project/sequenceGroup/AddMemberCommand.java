package uk.ac.gla.cvr.gluetools.core.command.project.sequenceGroup;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.groupMember.GroupMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.sequenceGroup.SequenceGroup;


@CommandClass( 
	commandWords={"add","member"}, 
	docoptUsages={"<sourceName> <sequenceID>", "(-w <whereClause> | -a)"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>  Qualify added sequences",
	    "-a, --allSequences                             Add all project sequences"},
	description="Add sequences as group members",
	metaTags={CmdMeta.updatesDatabase},
	furtherHelp=
	"If both <sourceName> and <sequenceID> are specified, a single sequence is added.\n"+
	"The whereClause, if specified, qualifies which sequences are added.\n"+
	"If allSequences is specified, all sequences in the project will be added.\n"+
	"Examples:\n"+
	"  add member localSource GW12371\n"+
	"  add member -a\n"+
	"  add member -w \"source.name = 'local'\"\n"+
	"  add member -w \"sequenceID like 'f%' and custom_field = 'value1'\"\n"+
	"  add member -w \"sequenceID = '3452467'\""
) 
public class AddMemberCommand extends GroupModeCommand<CreateResult> {

	public static final String SEQUENCE_ID = "sequenceID";
	public static final String SOURCE_NAME = "sourceName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_SEQUENCES = "allSequences";
	
	private Optional<String> sourceName;
	private Optional<String> sequenceID;
	private Optional<Expression> whereClause;
	private Boolean allSequences;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, SOURCE_NAME, false));
		sequenceID = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, SEQUENCE_ID, false));
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allSequences = PluginUtils.configureBooleanProperty(configElem, ALL_SEQUENCES, true);
		if(!(
				(sourceName.isPresent() && sequenceID.isPresent() && !whereClause.isPresent() && !allSequences)||
				(!sourceName.isPresent() && !sequenceID.isPresent() && !whereClause.isPresent() && allSequences)||
				(!sourceName.isPresent() && !sequenceID.isPresent() && whereClause.isPresent() && !allSequences)
			)) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, 
				"Either both sourceName and sequenceID or whereClause or allSequences must be specified");
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		SequenceGroup group = lookupGroup(cmdContext);
		List<Sequence> sequencesToAdd;
		if(whereClause.isPresent()) {
			SelectQuery selectQuery = new SelectQuery(Sequence.class, whereClause.get());
			sequencesToAdd = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
		} else if(allSequences) {
			SelectQuery selectQuery = new SelectQuery(Sequence.class);
			sequencesToAdd = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
		} else {
			sequencesToAdd = Arrays.asList(GlueDataObject.lookup(cmdContext, Sequence.class, 
					Sequence.pkMap(sourceName.get(), sequenceID.get())));
		}
		int added = 0;
		for(Sequence seq: sequencesToAdd) {
			GroupMember newMember = GlueDataObject.create(cmdContext, GroupMember.class, 
					GroupMember.pkMap(group.getName(), seq.getSource().getName(), seq.getSequenceID()), true);
			newMember.setGroup(group);
			newMember.setSequence(seq);
			added++;
		}
		if(added > 0) {
			group.setLastUpdateTime(System.currentTimeMillis());
		}
		cmdContext.commit();
		return new CreateResult(GroupMember.class, added);
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
