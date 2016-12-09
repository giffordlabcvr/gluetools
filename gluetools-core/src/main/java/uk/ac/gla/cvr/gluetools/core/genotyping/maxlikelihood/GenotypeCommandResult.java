package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood.AbstractGenotypeCommand.DetailLevel;

public class GenotypeCommandResult extends BaseTableResult<QueryGenotypingResult> {

	private static final int PERCENT_SCORE_PRECISION = 2;
	
	public GenotypeCommandResult(List<CladeCategory> cladeCategories, DetailLevel detailLevel, List<QueryGenotypingResult> genotypeResults) {
		super("genotypeCommandResult", genotypeResults, getColumns(cladeCategories, detailLevel));
	}

	@SuppressWarnings("unchecked")
	public static TableColumn<QueryGenotypingResult>[] getColumns(List<CladeCategory> cladeCategories, DetailLevel detailLevel) {
		List<TableColumn<QueryGenotypingResult>>
			columnsList = new ArrayList<TableColumn<QueryGenotypingResult>>();
		columnsList.add(column("queryName", gResult -> gResult.queryName));
		for(CladeCategory cladeCategory: cladeCategories) {
			String categoryName = cladeCategory.getName();
			columnsList.add(column(categoryName+"FinalClade", gResult -> gResult.getCladeCategoryResult(cladeCategory.getName()).finalClade));
			if(EnumSet.of(DetailLevel.MEDIUM, DetailLevel.HIGH).contains(detailLevel)) {
				columnsList.add(column(categoryName+"CladeBalance", gResult -> {
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
					if(buf.length() == 0) {
						return "-";
					}
					return buf.toString();
				}));
			}
			if(EnumSet.of(DetailLevel.HIGH).contains(detailLevel)) {
				columnsList.add(column(categoryName+"ClosestMemberAlignmentName", gResult -> gResult.getCladeCategoryResult(cladeCategory.getName()).closestMemberAlignmentName));
				columnsList.add(column(categoryName+"ClosestMemberSourceName", gResult -> gResult.getCladeCategoryResult(cladeCategory.getName()).closestMemberSourceName));
				columnsList.add(column(categoryName+"ClosestMemberSequenceID", gResult -> gResult.getCladeCategoryResult(cladeCategory.getName()).closestMemberSequenceID));
			}
		}
		return (TableColumn<QueryGenotypingResult>[]) columnsList.toArray(new TableColumn<?>[0]);
	}
}
