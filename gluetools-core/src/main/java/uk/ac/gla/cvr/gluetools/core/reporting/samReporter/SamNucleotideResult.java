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

public class SamNucleotideResult extends BaseTableResult<SamNucleotideResidueCount> {

	public static final String 
		SAM_REFERENCE_NT = "samRefNt",
		AC_REFERENCE_NT = "acRefNt",
		READS_WITH_A = "readsWithA",
		READS_WITH_C = "readsWithC",
		READS_WITH_G = "readsWithG",
		READS_WITH_T = "readsWithT";


	public SamNucleotideResult(List<SamNucleotideResidueCount> rowData) {
		super("samNucleotidesResult", 
				rowData,
				column(SAM_REFERENCE_NT, nrc -> nrc.getSamRefNt()),
				column(AC_REFERENCE_NT, nrc -> nrc.getRelatedRefNt()), 
				column(READS_WITH_A, nrc -> nrc.getReadsWithA()),
				column(READS_WITH_C, nrc -> nrc.getReadsWithC()), 
				column(READS_WITH_G, nrc -> nrc.getReadsWithG()), 
				column(READS_WITH_T, nrc -> nrc.getReadsWithT()));
	}

}
