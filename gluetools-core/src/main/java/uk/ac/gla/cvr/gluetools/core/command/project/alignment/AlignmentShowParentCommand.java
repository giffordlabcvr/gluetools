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
		return new AlignmentShowParentResult(lookupAlignment(cmdContext).getParent());
	}
	
	public class AlignmentShowParentResult extends MapResult {

		public AlignmentShowParentResult(Alignment parent) {
			super("alignmentShowParent", mapBuilder()
					.put(Alignment.PARENT_NAME_PATH, parent != null ? parent.getName() : null)
					.put(Alignment.PARENT_PROPERTY+"."+"renderedName", parent != null ? parent.getRenderedName() : null)
					);
		}


	}


}
