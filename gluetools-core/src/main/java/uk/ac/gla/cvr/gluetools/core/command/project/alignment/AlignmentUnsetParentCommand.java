package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;

@CommandClass( 
		commandWords={"unset", "parent"},
		docoptUsages={""},
		metaTags={CmdMeta.updatesDatabase},
		description="Unset the parent of this alignment"
	) 
public class AlignmentUnsetParentCommand extends AlignmentModeCommand<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		alignment.setParent(null);
		cmdContext.commit();
		return new OkResult();
	}

}
