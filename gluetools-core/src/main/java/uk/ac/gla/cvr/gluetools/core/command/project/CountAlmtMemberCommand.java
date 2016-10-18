package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;


@CommandClass( 
	commandWords={"count", "almt-member"},
	docoptUsages={"[-w <whereClause>]"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>           Qualify result set"},
	description="Count alignment members",
	furtherHelp=
	"The optional whereClause qualifies which alignment members are included.\n"+
	"Examples:\n"+
	"  count almt-member -w \"sequence.source.name = 'local'\"\n"+
	"  count almt-member -w \"alignment.name = 'local'\"\n"+
	"  count almt-member -w \"sequence.custom_field = 'value1'\""
) 
public class CountAlmtMemberCommand extends AbstractCountCTableCommand {
	
	public CountAlmtMemberCommand() {
		super();
		setTableName(ConfigurableTable.alignment_member.name());
	}

	@CompleterClass
	public static final class Completer extends CountCommandCompleter {
		public Completer() {
			super(ConfigurableTable.alignment_member.name());
		}
	}


}
