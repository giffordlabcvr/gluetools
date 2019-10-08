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
package uk.ac.gla.cvr.gluetools.core.genotyping;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;

public class GenotypingTableResult extends BaseTableResult<QueryGenotypingResult> {

	private static final int PERCENT_SCORE_PRECISION = 2;
	
	public GenotypingTableResult(List<? extends BaseCladeCategory> cladeCategories, DetailLevel detailLevel, List<QueryGenotypingResult> genotypeResults) {
		super("genotypeCommandResult", genotypeResults, getColumns(cladeCategories, detailLevel));
	}

	@SuppressWarnings("unchecked")
	public static TableColumn<QueryGenotypingResult>[] getColumns(List<? extends BaseCladeCategory> cladeCategories, DetailLevel detailLevel) {
		List<TableColumn<QueryGenotypingResult>>
			columnsList = new ArrayList<TableColumn<QueryGenotypingResult>>();
		columnsList.add(column("queryName", gResult -> gResult.queryName));
		for(BaseCladeCategory cladeCategory: cladeCategories) {
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

				columnsList.add(column(categoryName+"ClosestTargetAlignmentName", gResult -> gResult.getCladeCategoryResult(cladeCategory.getName()).closestTargetAlignmentName));
				columnsList.add(column(categoryName+"ClosestTargetSourceName", gResult -> gResult.getCladeCategoryResult(cladeCategory.getName()).closestTargetSourceName));
				columnsList.add(column(categoryName+"ClosestTargetSequenceID", gResult -> gResult.getCladeCategoryResult(cladeCategory.getName()).closestTargetSequenceID));
			}
		}
		return (TableColumn<QueryGenotypingResult>[]) columnsList.toArray(new TableColumn<?>[0]);
	}
}
