package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"member"},
	docoptUsages={"<sourceName> <sequenceID>", 
				"-w <whereClause>"},
	docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Specify based on field values"},
	description="Enter command mode for an alignment member", 
	furtherHelp=
	"The optional whereClause allows a member to be specified via its field values.\n"+
	"If this query returns multiple or zero members, the command fails.\n"+
	"Examples:\n"+
	"  member -w \"GB_PRIMARY_ACCESSION = 'GR195721'\"\n"+
	"  member mySource 12823121") 
@EnterModeCommandClass(
		commandModeClass = MemberMode.class)
public class MemberCommand extends AlignmentModeCommand  {

	private String sourceName;
	private String sequenceID;
	private Expression whereClause;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		sequenceID = PluginUtils.configureStringProperty(configElem, "sequenceID", false);
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
	public CommandResult execute(CommandContext cmdContext) {
		Sequence sequence;
		if(whereClause == null) {
			sequence = GlueDataObject.lookup(cmdContext.getObjectContext(), Sequence.class, 
					Sequence.pkMap(sourceName, sequenceID));
		} else {
			SelectQuery selectQuery = new SelectQuery(Sequence.class, whereClause);
			List<Sequence> sequences = GlueDataObject.query(cmdContext.getObjectContext(), Sequence.class, selectQuery);
			int numSeqs = sequences.size();
			if(numSeqs == 1) {
				sequence = sequences.get(0);
			} else if(numSeqs == 0) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "Query returned no sequences.");
			} else {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "Query returned multiple sequences.");
			} 
		}
		Project project = getAlignmentMode(cmdContext).getProject();
		cmdContext.pushCommandMode(new MemberMode(project, this, sequence.getSource().getName(), sequence.getSequenceID()));
		return CommandResult.OK;
	}

}
