package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.AbstractListCTableCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.AbstractListCTableCommand.AbstractListCTableDelegate;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;


@CommandClass(
		commandWords={"list", "var-almt-note"}, 
		docoptUsages={"[-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [<fieldName> ...]"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify result set",
				"-p <pageSize>, --pageSize <pageSize>           Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>     Limit max number of records",
		"-o <fetchOffset>, --fetchOffset <fetchOffset>  Record number offset"},
		description="List variation-alignment notes defined on this variation",
		furtherHelp=
		"The <pageSize> option is for performance tuning. The default page size\n"+
		"is 250 records.\n"+
		"The optional whereClause qualifies which alignments are displayed.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  list var-almt-note -w \"alignment.name like 'AL_3%'\"\n"+
		"  list var-almt-note -w \"CUSTOM_FIELD = 'value1'\"\n"+
		"  list var-almt-note alignment.name CUSTOM_FIELD")
public class VariationListAlmtNoteCommand extends VariationModeCommand<ListResult> {
	
	private AbstractListCTableDelegate<VarAlmtNote> listCTableDelegate = new AbstractListCTableDelegate<VarAlmtNote>();
	
	
	public VariationListAlmtNoteCommand() {
		super();
		listCTableDelegate.setcTable(ConfigurableTable.VAR_ALMT_NOTE);
		listCTableDelegate.setSortComparator(new VarAlmtNoteSortComparator());
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		listCTableDelegate.configure(pluginConfigContext, configElem);
		Optional<Expression> whereClause = listCTableDelegate.getWhereClause();
		Expression pathExp = 
				ExpressionFactory.matchExp(VarAlmtNote.REF_SEQ_NAME_PATH, getRefSeqName()).andExp(
				ExpressionFactory.matchExp(VarAlmtNote.FEATURE_NAME_PATH, getFeatureName()).andExp(
				ExpressionFactory.matchExp(VarAlmtNote.VARIATION_NAME_PATH, getVariationName())));
		if(whereClause.isPresent()) {
			whereClause = Optional.of(whereClause.get().andExp(pathExp));
		} else {
			whereClause = Optional.of(pathExp);
		}
		listCTableDelegate.setWhereClause(whereClause);
		if(listCTableDelegate.getFieldNames() == null) {
			listCTableDelegate.setFieldNames(
					Arrays.asList(VarAlmtNote.ALIGNMENT_NAME_PATH ));

		}
		

	}

	@Override
	public ListResult execute(CommandContext cmdContext) {
		return listCTableDelegate.execute(cmdContext);
	}
	
	@CompleterClass
	public static class Completer extends AbstractListCTableCommand.FieldNameCompleter {
		public Completer() {
			super(ConfigurableTable.VAR_ALMT_NOTE);
		}
	}


	public static class VarAlmtNoteSortComparator implements Comparator<VarAlmtNote> {
		@Override
		public int compare(VarAlmtNote o1, VarAlmtNote o2) {
			return(o1.getAlignment().getName().compareTo(o2.getAlignment().getName()));
		}
	}

	
}
