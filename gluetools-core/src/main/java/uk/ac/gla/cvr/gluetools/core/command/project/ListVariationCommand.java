package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.FeatureLocListVariationCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

@CommandClass(
		commandWords={"list", "variation"}, 
		docoptUsages={"[-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [<fieldName> ...]"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify result set",
				"-p <pageSize>, --pageSize <pageSize>           Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>     Limit max number of records",
		"-o <fetchOffset>, --fetchOffset <fetchOffset>  Record number offset"},
		description="List variations",
		furtherHelp=
		"The <pageSize> option is for performance tuning. The default page size\n"+
		"is 250 records.\n"+
		"The optional whereClause qualifies which variations are displayed.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  list variation -w \"name like 'NS%'\"\n"+
		"  list variation -w \"CUSTOM_FIELD = 'value1'\"\n"+
		"  list variation name CUSTOM_FIELD") 
public class ListVariationCommand extends AbstractListCTableCommand<Variation> {

	public ListVariationCommand() {
		super(ConfigurableTable.VARIATION);
		setSortComparator(new VariationSortComparator());
	}

	@CompleterClass
	public static final class Completer extends FieldNameCompleter {
		public Completer() {
			super(ConfigurableTable.VARIATION);
		}
	}

	public static class VariationSortComparator extends FeatureLocListVariationCommand.VariationSortComparator {
		@Override
		public int compare(Variation o1, Variation o2) {
			int refNameCmpResult = o1.getFeatureLoc().getReferenceSequence().getName().compareTo(o2.getFeatureLoc().getReferenceSequence().getName());
			if(refNameCmpResult != 0) {
				return refNameCmpResult;
			}
			int featureNameCmpResult = o1.getFeatureLoc().getFeature().getName().compareTo(o2.getFeatureLoc().getFeature().getName());
			if(featureNameCmpResult != 0) {
				return featureNameCmpResult;
			}
			return super.compare(o1, o2);
		}
	}

}