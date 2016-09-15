package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;

@CommandClass( 
		commandWords={"descendent-tree"},
		docoptUsages={""},
		description="Render the descendents of this alignment as a tree"
	) 
public class AlignmentDescendentTreeCommand extends AlignmentModeCommand<AlignmentDescendentTreeResult> {

	@Override
	public AlignmentDescendentTreeResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		return new AlignmentDescendentTreeResult(alignment);
	}
	

}
