package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"add","member"}, 
	docoptUsages={"<sourceName> <sequenceID>", "(-w <whereClause> | -a)"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>  Qualify added sequences",
	    "-a, --allSequences                             Add all project sequences"},
	description="Add sequences as alignment members",
	furtherHelp=
	"If both <sourceName> and <sequenceID> are specified, a single sequence is added.\n"+
	"The whereClause, if specified, qualifies which sequences are added.\n"+
	"If allSequences is specified, all sequences in the project will be added.\n"+
	"Examples:\n"+
	"  add member localSource GW12371\n"+
	"  add member -a\n"+
	"  add member -w \"source.name = 'local'\"\n"+
	"  add member -w \"sequenceID like 'f%' and CUSTOM_FIELD = 'value1'\"\n"+
	"  add member -w \"sequenceID = '3452467'\""
) 
public class AddMemberCommand extends AlignmentModeCommand<CreateResult> {

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
		ObjectContext objContext = cmdContext.getObjectContext();
		Alignment alignment = lookupAlignment(cmdContext);
		List<Sequence> sequencesToAdd;
		if(whereClause.isPresent()) {
			SelectQuery selectQuery = new SelectQuery(Sequence.class, whereClause.get());
			sequencesToAdd = GlueDataObject.query(objContext, Sequence.class, selectQuery);
		} else if(allSequences) {
			SelectQuery selectQuery = new SelectQuery(Sequence.class);
			sequencesToAdd = GlueDataObject.query(objContext, Sequence.class, selectQuery);
		} else {
			sequencesToAdd = Arrays.asList(GlueDataObject.lookup(objContext, Sequence.class, 
					Sequence.pkMap(sourceName.get(), sequenceID.get())));
		}
		int added = 0;
		for(Sequence seq: sequencesToAdd) {
			AlignmentMember newMember = GlueDataObject.create(objContext, AlignmentMember.class, 
					AlignmentMember.pkMap(alignment.getName(), seq.getSource().getName(), seq.getSequenceID()), true);
			newMember.setAlignment(alignment);
			newMember.setSequence(seq);
			added++;
		}
		cmdContext.commit();
		return new CreateResult(AlignmentMember.class, added);
	}

}
