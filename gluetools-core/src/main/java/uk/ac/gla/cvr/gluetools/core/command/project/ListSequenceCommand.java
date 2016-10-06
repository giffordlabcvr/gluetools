package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;


@CommandClass( 
	commandWords={"list", "sequence"},
	docoptUsages={"[-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [-s <sortProperties>] [<fieldName> ...]"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>           Qualify result set",
		"-p <pageSize>, --pageSize <pageSize>                    Tune ORM page size",
		"-l <fetchLimit>, --fetchLimit <fetchLimit>              Limit max number of records",
		"-o <fetchOffset>, --fetchOffset <fetchOffset>           Record number offset",
		"-s <sortProperties>, --sortProperties <sortProperties>  Comma-separated sort properties"},
	description="List sequences or sequence field values",
	furtherHelp=
	"The <pageSize> option is for performance tuning. The default page size\n"+
	"is 250 records.\n"+
	"The optional whereClause qualifies which sequences are displayed.\n"+
	"The optional sortProperties allows combined ascending/descending orderings, e.g. +property1,-property2.\n"+
	"Where fieldNames are specified, only these field values will be displayed.\n"+
	"Examples:\n"+
	"  list sequence -w \"source.name = 'local'\"\n"+
	"  list sequence -w \"sequenceID like 'f%' and custom_field = 'value1'\"\n"+
	"  list sequence sequenceID custom_field"
) 
public class ListSequenceCommand extends AbstractListCTableCommand {

	
	public ListSequenceCommand() {
		super();
		setTableName(ConfigurableTable.sequence.name());
	}

	@CompleterClass
	public static final class Completer extends ListCommandCompleter {
		public Completer() {
			super(ConfigurableTable.sequence.name());
		}
	}


}
