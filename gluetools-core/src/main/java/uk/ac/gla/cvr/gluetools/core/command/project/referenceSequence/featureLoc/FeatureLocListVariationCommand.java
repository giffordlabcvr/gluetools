package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

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
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;


@CommandClass(
		commandWords={"list", "variation"}, 
		docoptUsages={"[-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [<fieldName> ...]"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify result set",
				"-p <pageSize>, --pageSize <pageSize>           Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>     Limit max number of records",
		"-o <fetchOffset>, --fetchOffset <fetchOffset>  Record number offset"},
		description="List variations defined on this feature location",
		furtherHelp=
		"The <pageSize> option is for performance tuning. The default page size\n"+
		"is 250 records.\n"+
		"The optional whereClause qualifies which alignments are displayed.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  list variation -w \"name like 'NS%'\"\n"+
		"  list variation -w \"CUSTOM_FIELD = 'value1'\"\n"+
		"  list variation name CUSTOM_FIELD")
public class FeatureLocListVariationCommand extends FeatureLocModeCommand<ListResult> {
	
	private AbstractListCTableDelegate<Variation> listCTableDelegate = new AbstractListCTableDelegate<Variation>();
	
	
	public FeatureLocListVariationCommand() {
		super();
		listCTableDelegate.setcTable(ConfigurableTable.VARIATION);
		listCTableDelegate.setSortComparator(new VariationSortComparator());
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		listCTableDelegate.configure(pluginConfigContext, configElem);
		Optional<Expression> whereClause = listCTableDelegate.getWhereClause();
		Expression pathExp = ExpressionFactory.matchExp(Variation.FEATURE_NAME_PATH, getFeatureName())
		.andExp(ExpressionFactory.matchExp(Variation.REF_SEQ_NAME_PATH, getRefSeqName()));
		if(whereClause.isPresent()) {
			whereClause = Optional.of(whereClause.get().andExp(pathExp));
		} else {
			whereClause = Optional.of(pathExp);
		}
		listCTableDelegate.setWhereClause(whereClause);
		if(listCTableDelegate.getFieldNames() == null) {
			listCTableDelegate.setFieldNames(
					Arrays.asList(_Variation.NAME_PROPERTY, Variation.TRANSLATION_TYPE_PROPERTY, 
					Variation.REGEX_PROPERTY, _Variation.DESCRIPTION_PROPERTY ));

		}
		

	}

	@Override
	public ListResult execute(CommandContext cmdContext) {
		return listCTableDelegate.execute(cmdContext);
	}
	
	@CompleterClass
	public static class Completer extends AbstractListCTableCommand.FieldNameCompleter {
		public Completer() {
			super(ConfigurableTable.VARIATION);
		}
	}


	public static class VariationSortComparator implements Comparator<Variation> {
		@Override
		public int compare(Variation o1, Variation o2) {
			int formatCmpResult = 
					Integer.compare(o1.getTranslationFormat().ordinal(), o2.getTranslationFormat().ordinal());
			if(formatCmpResult != 0) {
				return formatCmpResult;
			}
			int refStartCmpResult = 
					Integer.compare(o1.getRefStart(), o2.getRefStart());
			if(refStartCmpResult != 0) {
				return refStartCmpResult;
			}
			return(o1.getName().compareTo(o2.getName()));
		}
	}

	
}
