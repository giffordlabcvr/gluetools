package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;

@CommandClass( 
		commandWords={"set","field", "sequence"}, 
		docoptUsages={"(-w <whereClause> | -a) <fieldName> <fieldValue> [-b <batchSize>]"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated sequences", 
				"-a, --allSequences                             Update all sequences",
				"-b <batchSize>, --batchSize <batchSize>        Update batch size" },
		description="Set a field's value for one or more sequences", 
		furtherHelp="Updates to the database are committed in batches, the default batch size is 250.") 
public class SetFieldSequenceCommand extends ProjectModeCommand<UpdateResult> {

	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		// TODO Auto-generated method stub
		return null;
	}

}
