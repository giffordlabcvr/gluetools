package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;

@CommandClass( 
		commandWords={"unset","field", "sequence"}, 
		docoptUsages={"(-w <whereClause> | -a) <fieldName>  [-b <batchSize>]"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated sequences", 
				"-a, --allSequences                             Update all sequences",
				"-b <batchSize>, --batchSize <batchSize>        Update batch size" },
		description="Unset a field's value for one or more sequences", 
		furtherHelp="Unsetting means reverting the field value to null. Updates to the database are committed in batches, the default batch size is 250.") 
public class UnsetFieldSequenceCommand extends ProjectModeCommand<UpdateResult> {

	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		// TODO Auto-generated method stub
		return null;
	}

}
