package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

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
	docoptUsages={"(-w <whereClause> | -a)"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>  Qualify added sequences",
	    "-a, --allSequences                             Add all project sequences"},
	description="Add sequences as members",
	furtherHelp=
	"The whereClause, if specified, qualifies which sequences are added.\n"+
	"If allSequences is specified, all sequences in the project will be added.\n"+
	"Examples:\n"+
	"  add member -a\n"+
	"  add member -w \"source.name = 'local'\"\n"+
	"  add member -w \"sequenceID like 'f%' and CUSTOM_FIELD = 'value1'\"\n"+
	"  add member -w \"sequenceID = '3452467'\""
) 
public class AddMemberCommand extends AlignmentModeCommand<CreateResult> {

	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_SEQUENCES = "allSequences";
	
	private Optional<Expression> whereClause;
	private Boolean allSequences;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allSequences = PluginUtils.configureBooleanProperty(configElem, ALL_SEQUENCES, true);
		if(!whereClause.isPresent() && !allSequences) {
			usageError();
		}
		if(whereClause.isPresent() && allSequences) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or allSequences must be specified, but not both");
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Alignment alignment = GlueDataObject.lookup(objContext, Alignment.class, 
				Alignment.pkMap(getAlignmentName()));
		SelectQuery selectQuery;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(Sequence.class, whereClause.get());
		} else {
			selectQuery = new SelectQuery(Sequence.class);
		}
		List<Sequence> sequencesToAdd = GlueDataObject.query(objContext, Sequence.class, selectQuery);
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
