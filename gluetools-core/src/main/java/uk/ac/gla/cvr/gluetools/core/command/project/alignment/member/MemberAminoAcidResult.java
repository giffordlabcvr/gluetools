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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class MemberAminoAcidResult extends BaseTableResult<LabeledQueryAminoAcid> {

	public static final String 
		CODON_LABEL = "codonLabel",
		MEMBER_NT = "memberNt",
		REL_REF_NT = "relRefNt",
		DEPENDENT_NTS = "dependentNts",
		CODON_NTS = "codonNts",
		AMINO_ACID = "aminoAcid",
		DEFINITE_AAS = "definiteAas",
		POSSIBLE_AAS = "possibleAas";


	public MemberAminoAcidResult(List<LabeledQueryAminoAcid> rowData) {
		super("memberAminoAcidsResult", 
				rowData, 
				column(CODON_LABEL, lqaa -> lqaa.getLabeledAminoAcid().getLabeledCodon().getCodonLabel()),
				column(MEMBER_NT, lqaa -> lqaa.getQueryNtStart()),
				column(REL_REF_NT, lqaa -> lqaa.getLabeledAminoAcid().getLabeledCodon().getNtStart()),
				column(DEPENDENT_NTS, lqaa -> lqaa.getDependentNts()),
				column(CODON_NTS, lqaa -> lqaa.getLabeledAminoAcid().getTranslationInfo().getTripletNtsString()),
				column(AMINO_ACID, lqaa -> lqaa.getLabeledAminoAcid().getAminoAcid()),
				column(DEFINITE_AAS, lqaa -> lqaa.getLabeledAminoAcid().getTranslationInfo().getDefiniteAasString()),
				column(POSSIBLE_AAS, lqaa -> lqaa.getLabeledAminoAcid().getTranslationInfo().getPossibleAasString()));
	}

}
