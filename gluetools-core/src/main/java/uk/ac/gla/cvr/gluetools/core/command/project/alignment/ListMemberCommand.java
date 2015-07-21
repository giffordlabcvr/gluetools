package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.ListSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords={"list", "member"},
		docoptUsages={"[-w <whereClause>] [<fieldName> ...]"},
		docoptOptions={
			"-w <whereClause>, --whereClause <whereClause>  Qualify result set"},
		description="List member sequences or field values",
		furtherHelp=
		"The optional whereClause qualifies which member sequences are displayed.\n"+
		"If whereClause is not specified, all member sequences are displayed.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  list member -w \"source.name = 'local'\"\n"+
		"  list member -w \"sequenceID like 'f%' and CUSTOM_FIELD = 'value1'\"\n"+
		"  list member sequenceID CUSTOM_FIELD"
	) 
public class ListMemberCommand extends AlignmentModeCommand {

	public static final String FIELD_NAME = "fieldName";
	public static final String WHERE_CLAUSE = "whereClause";
	private Optional<Expression> whereClause;
	private List<String> fieldNames;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		fieldNames = PluginUtils.configureStringsProperty(configElem, FIELD_NAME);
		if(fieldNames.isEmpty()) {
			fieldNames = null; // default fields
		}
	}

	
	@Override
	public CommandResult execute(CommandContext cmdContext) {
		getAlignmentMode(cmdContext).getProject().checkValidSequenceFieldNames(fieldNames);
		ObjectContext objContext = cmdContext.getObjectContext();
		Alignment alignment = GlueDataObject.lookup(objContext, Alignment.class, 
				Alignment.pkMap(getAlignmentName()));
		List<AlignmentMember> members = alignment.getMembers();
		List<AlignmentMember> membersToDisplay;
		if(whereClause.isPresent()) {
			Expression whereClauseExp = whereClause.get();
			membersToDisplay = members.stream().filter(m -> whereClauseExp.match(m.getSequence())).collect(Collectors.toList());
		} else {
			membersToDisplay = members;
		}
		return new ListResult(Sequence.class, 
				membersToDisplay.stream().map(AlignmentMember::getSequence).collect(Collectors.toList()), fieldNames);
	}

	@CompleterClass
	public static class Completer extends ListSequenceCommand.ListSequencesFieldCompleter {}
	
}
