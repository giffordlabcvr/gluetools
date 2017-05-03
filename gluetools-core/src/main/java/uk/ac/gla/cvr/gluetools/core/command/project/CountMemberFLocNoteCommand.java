package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;


@CommandClass( 
	commandWords={"count", "member-floc-note"},
	docoptUsages={"[-w <whereClause>]"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>           Qualify result set"},
	description="Count member-feature-location notes",
	furtherHelp=
	"The optional whereClause qualifies which member-feature-location notes are included.\n"+
	"Examples:\n"+
	"  count member-floc-note -w \"featureLoc.feature.name = 'NS3'\"\n"+
	"  count member-floc-note -w \"member.alignment.name = 'AL_1'\"" 
) 
public class CountMemberFLocNoteCommand extends AbstractCountCTableCommand {
	
	public CountMemberFLocNoteCommand() {
		super();
		setTableName(ConfigurableTable.member_floc_note.name());
	}

	@CompleterClass
	public static final class Completer extends CountCommandCompleter {
		public Completer() {
			super(ConfigurableTable.member_floc_note.name());
		}
	}


}
