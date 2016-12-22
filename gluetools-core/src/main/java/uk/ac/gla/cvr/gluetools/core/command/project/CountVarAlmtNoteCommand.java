package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;


@CommandClass( 
	commandWords={"count", "var-almt-note"},
	docoptUsages={"[-w <whereClause>]"},
	docoptOptions={
		"-w <whereClause>, --whereClause <whereClause>           Qualify result set"},
	description="Count variation-alignment notes",
	furtherHelp=
	"The optional whereClause qualifies which variation-alignment notes are included.\n"+
	"Examples:\n"+
	"  count var_almt_note -w \"alignment.name = 'AL_1'\"\n"+
	"  count var_almt_note -w \"variation.name = 'myVariation'\"" 
) 
public class CountVarAlmtNoteCommand extends AbstractCountCTableCommand {
	
	public CountVarAlmtNoteCommand() {
		super();
		setTableName(ConfigurableTable.var_almt_note.name());
	}

	@CompleterClass
	public static final class Completer extends CountCommandCompleter {
		public Completer() {
			super(ConfigurableTable.var_almt_note.name());
		}
	}


}
