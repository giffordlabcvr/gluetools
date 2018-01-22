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
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
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
		"  list variation -w \"custom_field = 'value1'\"\n"+
		"  list variation name custom_field")
public class FeatureLocListVariationCommand extends FeatureLocModeCommand<ListResult> {
	
	private AbstractListCTableDelegate listCTableDelegate = new AbstractListCTableDelegate();
	
	
	public FeatureLocListVariationCommand() {
		super();
		listCTableDelegate.setTableName(ConfigurableTable.variation.name());
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
					_Variation.DESCRIPTION_PROPERTY ));

		}
		

	}

	@Override
	public ListResult execute(CommandContext cmdContext) {
		return listCTableDelegate.execute(cmdContext);
	}
	
	@CompleterClass
	public static class Completer extends AbstractListCTableCommand.ListCommandCompleter {
		public Completer() {
			super(ConfigurableTable.variation.name());
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
			Integer refStart1 = o1.minLocStart();
			Integer refStart2 = o2.minLocStart();
			if(refStart1 != null && refStart2 != null) {
				int refStartCmpResult = Integer.compare(refStart1, refStart2);
				if(refStartCmpResult != 0) {
					return refStartCmpResult;
				}
			}
			return(o1.getName().compareTo(o2.getName()));
		}
	}

	
}
