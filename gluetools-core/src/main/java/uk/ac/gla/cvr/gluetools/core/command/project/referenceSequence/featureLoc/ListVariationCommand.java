package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.ListVariationCommand.ListVariationResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;


@CommandClass( 
		commandWords={"list", "variation"},
		docoptUsages={""},
		description="List feature variations"
	) 
public class ListVariationCommand extends FeatureLocModeCommand<ListVariationResult> {
	

	public static final String VARIATION_CATEGORIES = "variationCategories";


	@Override
	public ListVariationResult execute(CommandContext cmdContext) {
		ListResult listResult = CommandUtils.runListCommand(cmdContext, Variation.class, new SelectQuery(Variation.class, 
				ExpressionFactory
					.matchExp(Variation.FEATURE_NAME_PATH, getFeatureName())
					.andExp(ExpressionFactory
							.matchExp(Variation.REF_SEQ_NAME_PATH, getRefSeqName())
				)));
		List<Map<String, Object>> rowData = listResult.asListOfMaps();
		for(Map<String, Object> row: rowData) {
			Variation variation = GlueDataObject.lookup(cmdContext, Variation.class, 
					Variation.pkMap(getRefSeqName(), getFeatureName(), (String) row.get(Variation.NAME_PROPERTY)));
			
			List<String> vcatNames = variation.getVcatMemberships().stream().map(vcm -> vcm.getCategory().getName()).collect(Collectors.toList());
			if(!vcatNames.isEmpty()) {
				row.put(VARIATION_CATEGORIES, String.join(", ", vcatNames));
			}
		}
		return new ListVariationResult(rowData);
	}

	
	public static class ListVariationResult extends TableResult {

		public ListVariationResult(List<Map<String, Object>> rowData) {
			super("listVariationResult", 
					Arrays.asList(_Variation.NAME_PROPERTY, Variation.TRANSCRIPTION_TYPE_PROPERTY, 
							Variation.REGEX_PROPERTY, _Variation.DESCRIPTION_PROPERTY, VARIATION_CATEGORIES), rowData);
		}

	}

}
