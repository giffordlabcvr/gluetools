package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;


@CommandClass(
		commandWords={"list", "reference"}, 
		docoptUsages={"[-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [<fieldName> ...]"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify result set",
				"-p <pageSize>, --pageSize <pageSize>           Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>     Limit max number of records",
		"-o <fetchOffset>, --fetchOffset <fetchOffset>  Record number offset"},
		description="List references",
		furtherHelp=
		"The <pageSize> option is for performance tuning. The default page size\n"+
		"is 250 records.\n"+
		"The optional whereClause qualifies which references are displayed.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  list reference -w \"name like 'NS%'\"\n"+
		"  list reference -w \"custom_field = 'value1'\"\n"+
		"  list reference name custom_field") 
public class ListReferenceSequenceCommand extends AbstractListCTableCommand {

	public ListReferenceSequenceCommand() {
		super();
		setTableName(ConfigurableTable.reference.name());
	}

	@CompleterClass
	public static final class Completer extends FieldNameCompleter {
		public Completer() {
			super(ConfigurableTable.reference.name());
		}
	}

}
