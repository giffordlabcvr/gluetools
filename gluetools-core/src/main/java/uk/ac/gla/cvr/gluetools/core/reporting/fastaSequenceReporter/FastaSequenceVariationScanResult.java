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
package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanMatchResultRow;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanRenderHints;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

public class FastaSequenceVariationScanResult extends BaseTableResult<VariationScanMatchResultRow> {

	public static final String 
		REF_SEQ_NAME = "referenceName",
		FEATURE_NAME = "featureName",
		VARIATION_NAME = "variationName";

	public FastaSequenceVariationScanResult(VariationScanRenderHints renderHints, List<VariationScanResult<?>> rowData) {
		/* RESTORE_XXXX
		super("fastaSequenceVariationScanResult", 
				renderHints.scanResultsToResultRows(rowData), 
				renderHints.generateResultColumns(
					column(REF_SEQ_NAME, vsrr -> vsrr.getVariationReferenceName()),
					column(FEATURE_NAME, vsrr -> vsrr.getVariationFeatureName()),
					column(VARIATION_NAME, vsrr -> vsrr.getVariationName())
				));
		*/ super(null, null);
	}

}
