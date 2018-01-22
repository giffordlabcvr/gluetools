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
package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class SamVariationScanResult extends BaseTableResult<VariationScanReadCount> {

	public static final String 
		REF_SEQ_NAME = "referenceName",
		FEATURE_NAME = "featureName",
		VARIATION_NAME = "variationName",
		READS_PRESENT = "readsPresent",
		PCT_PRESENT = "pctPresent",
		READS_ABSENT = "readsAbsent",
		PCT_ABSENT = "pctAbsent";
	
	
	public SamVariationScanResult(List<VariationScanReadCount> rowData) {
		super("samVariationsScanResult", 
				rowData,
				column(REF_SEQ_NAME, vsrc -> vsrc.getVariationReferenceName()), 
				column(FEATURE_NAME, vsrc -> vsrc.getVariationFeatureName()), 
				column(VARIATION_NAME, vsrc -> vsrc.getVariationName()), 
				column(READS_PRESENT, vsrc -> vsrc.getReadsWherePresent()), 
				column(PCT_PRESENT, vsrc -> vsrc.getPctWherePresent()), 
				column(READS_ABSENT, vsrc -> vsrc.getReadsWhereAbsent()),
				column(PCT_ABSENT, vsrc -> vsrc.getPctWhereAbsent()));
	}

}
