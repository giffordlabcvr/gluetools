/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Comparator;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;


@CommandClass(
	commandWords={"list", "feature-location"}, 
			docoptUsages={"[-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [-s <sortProperties>] [<fieldName> ...]"},
			docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>           Qualify result set",
				"-p <pageSize>, --pageSize <pageSize>                    Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>              Limit max number of records",
				"-o <fetchOffset>, --fetchOffset <fetchOffset>           Record number offset",
				"-s <sortProperties>, --sortProperties <sortProperties>  Comma-separated sort properties"},
			description="List feature locations across different reference sequences",
			furtherHelp=
			"The <pageSize> option is for performance tuning. The default page size\n"+
			"is 250 records.\n"+
			"The optional whereClause qualifies which feature-locations are displayed.\n"+
			"The optional sortProperties allows combined ascending/descending orderings, e.g. +property1,-property2.\n"+
			"Where fieldNames are specified, only these field values will be displayed.\n"+
			"Examples:\n"+
			"  list feature-location -w \"name like 'NS%'\"\n"+
			"  list feature-location -w \"custom_field = 'value1'\"\n"+
			"  list feature-location referenceSequence.name feature.name custom_field") 
public class ListFeatureLocationCommand extends AbstractListCTableCommand {

	public ListFeatureLocationCommand() {
		super();
		setTableName(ConfigurableTable.feature_location.name());
		setSortComparator(new Comparator<FeatureLocation>(){
			@Override
			public int compare(FeatureLocation fl1, FeatureLocation fl2) {
				int comp = fl1.getReferenceSequence().getName().compareTo(fl2.getReferenceSequence().getName());
				if(comp != 0) {
					return comp;
				}
				comp = fl1.getFeature().getName().compareTo(fl2.getFeature().getName());
				if(comp != 0) {
					return comp;
				}
				return Feature.compareDisplayOrderKeyLists(fl1.getFeature().getDisplayOrderKeyList(), fl2.getFeature().getDisplayOrderKeyList());
			}
		});
	}

	@CompleterClass
	public static final class Completer extends ListCommandCompleter {
		public Completer() {
			super(ConfigurableTable.feature_location.name());
		}
	}

}
