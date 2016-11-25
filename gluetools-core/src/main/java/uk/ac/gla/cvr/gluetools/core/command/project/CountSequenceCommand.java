package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;


@CommandClass( 
	commandWords={"count", "sequence"},
	docoptUsages={"[-w <whereClause>]"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>           Qualify result set"},
	description="Count sequences",
	furtherHelp=
	"The optional whereClause qualifies which sequences are included.\n"+
	"Examples:\n"+
	"  count sequence -w \"source.name = 'local'\"\n"+
	"  count sequence -w \"sequenceID like 'f%' and custom_field = 'value1'\""
) 
public class CountSequenceCommand extends AbstractCountCTableCommand {

	
	public CountSequenceCommand() {
		super();
		setTableName(ConfigurableTable.sequence.name());
	}

	@CompleterClass
	public static final class Completer extends CountCommandCompleter {
		public Completer() {
			super(ConfigurableTable.sequence.name());
		}
	}


}
