package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Comparator;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;


@CommandClass(
	commandWords={"list", "feature"}, 
			docoptUsages={"[-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [<fieldName> ...]"},
			docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify result set",
				"-p <pageSize>, --pageSize <pageSize>           Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>     Limit max number of records",
				"-o <fetchOffset>, --fetchOffset <fetchOffset>  Record number offset"},
			description="List genome features",
			furtherHelp=
			"The <pageSize> option is for performance tuning. The default page size\n"+
			"is 250 records.\n"+
			"The optional whereClause qualifies which features are displayed.\n"+
			"Where fieldNames are specified, only these field values will be displayed.\n"+
			"Examples:\n"+
			"  list feature -w \"name like 'NS%'\"\n"+
			"  list feature -w \"custom_field = 'value1'\"\n"+
			"  list feature name custom_field") 
public class ListFeatureCommand extends AbstractListCTableCommand {

	public ListFeatureCommand() {
		super();
		setTableName(ConfigurableTable.feature.name());
		setSortComparator(new Comparator<Feature>(){
			@Override
			public int compare(Feature f1, Feature f2) {
				return Feature.compareDisplayOrderKeyLists(f1.getDisplayOrderKeyList(), f2.getDisplayOrderKeyList());
			}
		});
	}

	@CompleterClass
	public static final class Completer extends FieldNameCompleter {
		public Completer() {
			super(ConfigurableTable.feature.name());
		}
	}

}
