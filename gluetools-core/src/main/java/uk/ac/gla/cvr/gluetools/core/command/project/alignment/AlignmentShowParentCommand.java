package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;

@CommandClass( 
		commandWords={"show", "parent"},
		docoptUsages={""},
		description="Show the parent of this alignment"
	) 
public class AlignmentShowParentCommand extends AlignmentModeCommand<AlignmentShowParentCommand.AlignmentShowParentResult> {

	@Override
	public AlignmentShowParentResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		String parentName = null;
		Alignment parent = alignment.getParent();
		if(parent != null) {
			parentName = parent.getName();
		}
		return new AlignmentShowParentResult(parentName);
	}
	
	public class AlignmentShowParentResult extends MapResult {

		public AlignmentShowParentResult(String parentName) {
			super("alignmentShowParent", mapBuilder().put(Alignment.PARENT_NAME_PATH, parentName));
		}


	}


}
