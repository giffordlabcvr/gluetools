package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;

public class GenotypeCommandResult extends BaseTableResult<QueryGenotypingResult> {

	private static final int PERCENT_SCORE_PRECISION = 2;
	
	public GenotypeCommandResult(List<CladeCategory> cladeCategories, List<QueryGenotypingResult> genotypeResults) {
		super("genotypeCommandResult", genotypeResults, getColumns(cladeCategories));
	}

	@SuppressWarnings("unchecked")
	public static TableColumn<QueryGenotypingResult>[] getColumns(List<CladeCategory> cladeCategories) {
		List<TableColumn<QueryGenotypingResult>>
			columnsList = new ArrayList<TableColumn<QueryGenotypingResult>>();
		columnsList.add(column("queryName", gResult -> gResult.queryName));
		for(CladeCategory cladeCategory: cladeCategories) {
			String categoryName = cladeCategory.getName();
			columnsList.add(column(categoryName, gResult -> {
				Optional<QueryCladeCategoryResult> cladeCatResult = 
						gResult.queryCladeCategoryResult.stream()
						.filter(qccr -> qccr.categoryName.equals(categoryName))
						.findFirst();
				if(!cladeCatResult.isPresent()) {
					return null;
				}
				List<QueryCladeResult> queryCladeResults = cladeCatResult.get().queryCladeResult;
				StringBuffer buf = new StringBuffer();
				for(int i = 0; i < queryCladeResults.size(); i++) {
					if(i > 0) {
						buf.append(", ");
					}
					buf.append(queryCladeResults.get(i).cladeName)
						.append(":")
						.append(String.format("%."+PERCENT_SCORE_PRECISION+"f", queryCladeResults.get(i).percentScore.doubleValue()))
						.append("%");
				}
				return buf.toString();
			}));
			
		}
		return (TableColumn<QueryGenotypingResult>[]) columnsList.toArray(new TableColumn<?>[0]);
	}
}
