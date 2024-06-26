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

public class SamAminoAcidResult extends BaseTableResult<LabeledAminoAcidReadCount> {

	public static final String 
		FEATURE = "feature",
		CODON_LABEL = "codonLabel",
		SAM_REF_NT = "samRefNt",
		REL_REF_NT = "relRefNt",
		AMINO_ACID = "aminoAcid",
		READS_WITH_AA = "readsWithAA",
		READS_WITH_DIFFERENT_AA = "readsWithDifferentAA",
		PERCENT_AA_READS = "pctAaReads";


	public SamAminoAcidResult(List<LabeledAminoAcidReadCount> rows) {
		super("samAminoAcidsResult", 
				rows,
				column(FEATURE, laarc -> laarc.getLabeledCodon().getFeatureName()),
				column(CODON_LABEL, laarc -> laarc.getLabeledCodon().getCodonLabel()),
				column(SAM_REF_NT, laarc -> laarc.getSamRefNt()), 
				column(REL_REF_NT, laarc -> laarc.getLabeledCodon().getNtStart()),
				column(AMINO_ACID, laarc -> laarc.getAminoAcid()), 
				column(READS_WITH_AA, laarc -> laarc.getReadsWithAminoAcid()), 
				column(READS_WITH_DIFFERENT_AA, laarc -> laarc.getReadsWithDifferentAminoAcid()), 
				column(PERCENT_AA_READS, laarc -> laarc.getPercentReadsWithAminoAcid()));
	}

}
