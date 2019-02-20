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

public class SamCodonTripletsResult extends BaseTableResult<LabeledCodonTripletReadCount> {

	public static final String 
		FEATURE = "feature",
		CODON_LABEL = "codonLabel",
		SAM_REF_NT = "samRefNt",
		REL_REF_NT = "relRefNt",
		TRIPLET = "triplet",
		AMINO_ACID = "aminoAcid",
		READS_WITH_TRIPLET = "readsWithTriplet",
		READS_WITH_DIFFERENT_TRIPLET = "readsWithDifferentTriplet",
		PERCENT_TRIPLET_READS = "pctTripletReads";


	public SamCodonTripletsResult(List<LabeledCodonTripletReadCount> rows) {
		super("samCodonTripletsResult", 
				rows,
				column(FEATURE, lctrc -> lctrc.getLabeledCodon().getFeatureName()),
				column(CODON_LABEL, lctrc -> lctrc.getLabeledCodon().getCodonLabel()),
				column(SAM_REF_NT, lctrc -> lctrc.getSamRefNt()), 
				column(REL_REF_NT, lctrc -> lctrc.getLabeledCodon().getNtStart()),
				column(TRIPLET, lctrc -> lctrc.getTriplet()), 
				column(AMINO_ACID, lctrc -> lctrc.getAminoAcid()), 
				column(READS_WITH_TRIPLET, lctrc -> lctrc.getReadsWithTriplet()), 
				column(READS_WITH_DIFFERENT_TRIPLET, lctrc -> lctrc.getReadsWithDifferentTriplet()), 
				column(PERCENT_TRIPLET_READS, lctrc -> lctrc.getPercentReadsWithTriplet()));
	}

}
