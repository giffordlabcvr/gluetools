package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;


@CommandClass( 
	commandWords={"count", "variation"},
	docoptUsages={"[-w <whereClause>]"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>           Qualify result set"},
	description="Count variations",
	furtherHelp=
	"The optional whereClause qualifies which variations are included.\n"+
	"Examples:\n"+
	"  count variation -w \"name like 'NS%'\"\n"+
	"  count variation -w \"custom_field = 'value1'\"" 
) 
public class CountVariationCommand extends AbstractCountCTableCommand {
	
	public CountVariationCommand() {
		super();
		setTableName(ConfigurableTable.variation.name());
	}

	@CompleterClass
	public static final class Completer extends CountCommandCompleter {
		public Completer() {
			super(ConfigurableTable.variation.name());
		}
	}


}
