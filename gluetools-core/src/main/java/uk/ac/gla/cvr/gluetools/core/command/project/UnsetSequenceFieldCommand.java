package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;

@CommandClass( 
		commandWords={"unset", "sequence", "field"}, 
		docoptUsages={"(-w <whereClause> | -a) <fieldName>  [-b <batchSize>]"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated sequences", 
				"-a, --allSequences                             Update all sequences",
				"-b <batchSize>, --batchSize <batchSize>        Update batch size" },
		description="Unset a field's value for one or more sequences", 
		furtherHelp="Unsetting means reverting the field value to null. Updates to the database are committed in batches, the default batch size is 250.") 
public class UnsetSequenceFieldCommand extends MultiSequenceFieldUpdateCommand {

	@Override
	protected void updateSequence(CommandContext cmdContext, Sequence sequence, String fieldName) {
		sequence.writeProperty(fieldName, null);
	}

	@CompleterClass
	public static class Completer extends SequenceFieldNameCompleter {}

}
