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
package uk.ac.gla.cvr.gluetools.core.translation;

import org.junit.Assert;
import org.junit.Test;

public class TestNtsToAAs {

	@Test
	public void ntsToAaTest() {
		aaTest("UUU", 'F');
		aaTest("UUC", 'F');
		aaTest("UUA", 'L');
		aaTest("UUG", 'L');

		aaTest("UCU", 'S');
		aaTest("UCC", 'S');
		aaTest("UCA", 'S');
		aaTest("UCG", 'S');

		aaTest("UAU", 'Y');
		aaTest("UAC", 'Y');
		aaTest("UAA", '*');
		aaTest("UAG", '*');

		aaTest("UGU", 'C');
		aaTest("UGC", 'C');
		aaTest("UGA", '*');
		aaTest("UGG", 'W');

		aaTest("CUU", 'L');
		aaTest("CUC", 'L');
		aaTest("CUA", 'L');
		aaTest("CUG", 'L');

		aaTest("CCU", 'P');
		aaTest("CCC", 'P');
		aaTest("CCA", 'P');
		aaTest("CCG", 'P');

		aaTest("CAU", 'H');
		aaTest("CAC", 'H');
		aaTest("CAA", 'Q');
		aaTest("CAG", 'Q');

		aaTest("CGU", 'R');
		aaTest("CGC", 'R');
		aaTest("CGA", 'R');
		aaTest("CGG", 'R');

		aaTest("AUU", 'I');
		aaTest("AUC", 'I');
		aaTest("AUA", 'I');
		aaTest("AUG", 'M');

		aaTest("ACU", 'T');
		aaTest("ACC", 'T');
		aaTest("ACA", 'T');
		aaTest("ACG", 'T');

		aaTest("AAU", 'N');
		aaTest("AAC", 'N');
		aaTest("AAA", 'K');
		aaTest("AAG", 'K');

		aaTest("AGU", 'S');
		aaTest("AGC", 'S');
		aaTest("AGA", 'R');
		aaTest("AGG", 'R');


		aaTest("GUU", 'V');
		aaTest("GUC", 'V');
		aaTest("GUA", 'V');
		aaTest("GUG", 'V');

		aaTest("GCU", 'A');
		aaTest("GCC", 'A');
		aaTest("GCA", 'A');
		aaTest("GCG", 'A');

		aaTest("GAU", 'D');
		aaTest("GAC", 'D');
		aaTest("GAA", 'E');
		aaTest("GAG", 'E');

		aaTest("GGU", 'G');
		aaTest("GGC", 'G');
		aaTest("GGA", 'G');
		aaTest("GGG", 'G');

	}

	
	@Test
	public void ntsToAaTest2() {
		aaTest("TTT", 'F');
		aaTest("TTC", 'F');
		aaTest("TTA", 'L');
		aaTest("TTG", 'L');

		aaTest("TCT", 'S');
		aaTest("TCC", 'S');
		aaTest("TCA", 'S');
		aaTest("TCG", 'S');

		aaTest("TAT", 'Y');
		aaTest("TAC", 'Y');
		aaTest("TAA", '*');
		aaTest("TAG", '*');

		aaTest("TGT", 'C');
		aaTest("TGC", 'C');
		aaTest("TGA", '*');
		aaTest("TGG", 'W');

		aaTest("CTT", 'L');
		aaTest("CTC", 'L');
		aaTest("CTA", 'L');
		aaTest("CTG", 'L');

		aaTest("CCT", 'P');
		aaTest("CCC", 'P');
		aaTest("CCA", 'P');
		aaTest("CCG", 'P');

		aaTest("CAT", 'H');
		aaTest("CAC", 'H');
		aaTest("CAA", 'Q');
		aaTest("CAG", 'Q');

		aaTest("CGT", 'R');
		aaTest("CGC", 'R');
		aaTest("CGA", 'R');
		aaTest("CGG", 'R');

		aaTest("ATT", 'I');
		aaTest("ATC", 'I');
		aaTest("ATA", 'I');
		aaTest("ATG", 'M');

		aaTest("ACT", 'T');
		aaTest("ACC", 'T');
		aaTest("ACA", 'T');
		aaTest("ACG", 'T');

		aaTest("AAT", 'N');
		aaTest("AAC", 'N');
		aaTest("AAA", 'K');
		aaTest("AAG", 'K');

		aaTest("AGT", 'S');
		aaTest("AGC", 'S');
		aaTest("AGA", 'R');
		aaTest("AGG", 'R');


		aaTest("GTT", 'V');
		aaTest("GTC", 'V');
		aaTest("GTA", 'V');
		aaTest("GTG", 'V');

		aaTest("GCT", 'A');
		aaTest("GCC", 'A');
		aaTest("GCA", 'A');
		aaTest("GCG", 'A');

		aaTest("GAT", 'D');
		aaTest("GAC", 'D');
		aaTest("GAA", 'E');
		aaTest("GAG", 'E');

		aaTest("GGT", 'G');
		aaTest("GGC", 'G');
		aaTest("GGA", 'G');
		aaTest("GGG", 'G');

	}

	

	
	@Test
	public void ntsToAaTest3() {
		aaTest("TTY", 'F');
		aaTest("TTR", 'L');

		aaTest("TCN", 'S');

		aaTest("TAY", 'Y');
		aaTest("TAR", '*');

		aaTest("TGY", 'C');
		aaTest("TRA", '*');

		aaTest("CTN", 'L');

		aaTest("CCN", 'P');

		aaTest("CAY", 'H');
		aaTest("CAR", 'Q');

		aaTest("CGN", 'R');

		aaTest("ATY", 'I');
		aaTest("ATH", 'I');

		aaTest("ACN", 'T');

		aaTest("AAY", 'N');
		aaTest("AAR", 'K');

		aaTest("AGY", 'S');
		aaTest("AGR", 'R');


		aaTest("GTN", 'V');

		aaTest("GCN", 'A');

		aaTest("GAY", 'D');
		aaTest("GAR", 'E');

		aaTest("GGN", 'G');
		aaTest("YUR", 'L');
		aaTest("MGR", 'R');
		
		
	}
	
	private static void aaTest(String nts, char expectedAA) {
		Assert.assertEquals(Character.toString(expectedAA), Character.toString(CodonTableUtils.translateToSingleChar(nts.toCharArray())));
	}

}
