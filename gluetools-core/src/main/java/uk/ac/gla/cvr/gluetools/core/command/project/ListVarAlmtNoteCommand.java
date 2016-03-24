package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;

@CommandClass(
		commandWords={"list", "var-almt-note"}, 
		docoptUsages={"[-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [<fieldName> ...]"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify result set",
				"-p <pageSize>, --pageSize <pageSize>           Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>     Limit max number of records",
		"-o <fetchOffset>, --fetchOffset <fetchOffset>  Record number offset"},
		description="List variation-alignment notes",
		furtherHelp=
		"The <pageSize> option is for performance tuning. The default page size\n"+
		"is 250 records.\n"+
		"The optional whereClause qualifies which alignments are displayed.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  list var-almt-note -w \"alignment.name like 'AL_3%'\"\n"+
		"  list var-almt-note -w \"custom_field = 'value1'\"\n"+
		"  list var-almt-note alignment.name custom_field") 
public class ListVarAlmtNoteCommand extends AbstractListCTableCommand<VarAlmtNote> {

	public ListVarAlmtNoteCommand() {
		super(ConfigurableTable.var_almt_note);
	}

	@CompleterClass
	public static final class Completer extends FieldNameCompleter {
		public Completer() {
			super(ConfigurableTable.var_almt_note);
		}
	}

}
