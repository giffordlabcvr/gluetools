package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;

@CommandClass( 
		commandWords={"list", "descendent"},
		docoptUsages={""},
		description="List the descendents of this alignment"
	) 
public class AlignmentListDescendentCommand extends AlignmentModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		Alignment alignment = lookupAlignment(cmdContext);
		List<Alignment> descendents = alignment.getDescendents();
		List<String> columnHeaders = Arrays.asList(Alignment.NAME_PROPERTY, Alignment.REF_SEQ_NAME_PATH);
		return new ListResult(Alignment.class, descendents, columnHeaders);
	}
	

}