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

import java.util.LinkedList;
import java.util.List;

/**
 * The role of this class is to perform fast translation of a char array of 3 nucleotide 
 * characters into the corresponding protein character, using this method:
 *
 * public char translate(char[] bases)
 *
 * The input array may contain IUPAC ambiguity codes. Each IUPAC nucleotide code is mapped to an
 * integer between 0 and 16. A precomputed 3-dimensional array is used to store the results
 * of mapping any combination of 3 IUPAC codes to the appropriate amino acid code.
 *
 *
 * There are 
 *
 *
 */

public class CodonTableUtils {

	
	
	// see https://www.ncbi.nlm.nih.gov/Taxonomy/Utils/wprintgc.cgi?chapter=cgencodes
	private static String standardCodonTable =
		"  AAs  = FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG\n"+
		"  Starts = ---M------**--*----M---------------M----------------------------\n"+
		"  Base1  = TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG\n"+
		"  Base2  = TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG\n"+
		"  Base3  = TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG\n";
	
	// 3-dimensional array mapping triplet of concrete NT (integer representation)
	// to amino acid (integer representation);
	private static int[][][] concreteNtTripletToAa = new int[4][4][4];
	
	// array of lists of int[], mapping amino acid (integer representation)
	// to list of concrete NT triplets which code for it.
	private static List<?>[] aaToConcreteNtTriplets = new List<?>[21];
	
	// parse the codon table, populating concreteNtTripletToAa
	static {
		String codonTable = standardCodonTable;
		String[] codonTableLines = codonTable.split("\\n");
		String aAs = codonTableLines[0].substring(codonTableLines[0].lastIndexOf(' ')+1);
		String base1 = codonTableLines[2].substring(codonTableLines[2].lastIndexOf(' ')+1);
		String base2 = codonTableLines[3].substring(codonTableLines[3].lastIndexOf(' ')+1);
		String base3 = codonTableLines[4].substring(codonTableLines[4].lastIndexOf(' ')+1);
		
		for(int i = 0; i < 64; i++) {
			int intAa = ResidueUtils.aaToInt(aAs.charAt(i));
			int intConcreteNt1 = ResidueUtils.concreteNtToInt(base1.charAt(i));
			int intConcreteNt2 = ResidueUtils.concreteNtToInt(base2.charAt(i));
			int intConcreteNt3 = ResidueUtils.concreteNtToInt(base3.charAt(i));
			concreteNtTripletToAa[intConcreteNt1][intConcreteNt2][intConcreteNt3] = intAa;
			@SuppressWarnings("unchecked")
			List<int[]> aaTripletsList = (List<int[]>) aaToConcreteNtTriplets[intAa];
			if(aaTripletsList == null) {
				aaTripletsList = new LinkedList<int[]>();
				aaToConcreteNtTriplets[intAa] = aaTripletsList;
			}
			aaTripletsList.add(new int[]{intConcreteNt1, intConcreteNt2, intConcreteNt3});
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<int[]> aaToConcreteNtTriplets(int aa) {
		return (List<int[]>) aaToConcreteNtTriplets[aa];
	}

	public static int concreteNtTripletToAa(int concreteNt1, int concreteNt2, int concreteNt3) {
		return concreteNtTripletToAa[concreteNt1][concreteNt2][concreteNt3];
	}
	
}
