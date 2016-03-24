package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;


@CommandClass( 
	commandWords={"list", "sequence"},
	docoptUsages={"[-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [<fieldName> ...]"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>  Qualify result set",
		"-p <pageSize>, --pageSize <pageSize>           Tune ORM page size",
		"-l <fetchLimit>, --fetchLimit <fetchLimit>     Limit max number of records",
		"-o <fetchOffset>, --fetchOffset <fetchOffset>  Record number offset"},
	description="List sequences or sequence field values",
	furtherHelp=
	"The <pageSize> option is for performance tuning. The default page size\n"+
	"is 250 records.\n"+
	"The optional whereClause qualifies which sequences are displayed.\n"+
	"Where fieldNames are specified, only these field values will be displayed.\n"+
	"Examples:\n"+
	"  list sequence -w \"source.name = 'local'\"\n"+
	"  list sequence -w \"sequenceID like 'f%' and custom_field = 'value1'\"\n"+
	"  list sequence sequenceID custom_field"
) 
public class ListSequenceCommand extends AbstractListCTableCommand<Sequence> {

	
	public ListSequenceCommand() {
		super(ConfigurableTable.sequence);
	}

	@CompleterClass
	public static final class Completer extends FieldNameCompleter {
		public Completer() {
			super(ConfigurableTable.sequence);
		}
	}


}
