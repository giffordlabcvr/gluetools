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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationException.Code;

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
 */

public class CodonTableUtils {

	// see https://www.ncbi.nlm.nih.gov/Taxonomy/Utils/wprintgc.cgi?chapter=cgencodes
	private static String standardCodonTable =
		"  AAs  = FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG\n"+
		"  Starts = ---M------**--*----M---------------M----------------------------\n"+
		"  Base1  = TTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGG\n"+
		"  Base2  = TTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGGTTTTCCCCAAAAGGGG\n"+
		"  Base3  = TCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAGTCAG\n";
		
	private static LinkedHashMap<String, String> standardCodonTableMap = new LinkedHashMap<String, String>();
	static {
		String codonTable = standardCodonTable;
		String[] codonTableLines = codonTable.split("\\n");
		String aAs = codonTableLines[0].substring(codonTableLines[0].lastIndexOf(' ')+1);
		String base1 = codonTableLines[2].substring(codonTableLines[2].lastIndexOf(' ')+1);
		String base2 = codonTableLines[3].substring(codonTableLines[3].lastIndexOf(' ')+1);
		String base3 = codonTableLines[4].substring(codonTableLines[4].lastIndexOf(' ')+1);
		
		for(int i = 0; i < 64; i++) {
			char[] triplet = new char[3];
			triplet[0] = base1.charAt(i);
			triplet[1] = base2.charAt(i);
			triplet[2] = base3.charAt(i);
			standardCodonTableMap.put(new String(triplet), Character.toString(aAs.charAt(i)));
		}
	}
	
	private static char[][][] standardCodonTableTripletToAa = new char[16][16][16];
	private static String ntChars = "ACGTURYKMSWBDHVN";
	
	// populate 3d array for a specific combination of 3 IUPAC codes.
	// this is done by iterating over all possible triplets concrete (ACGT) nucleotides
	// which could underly the 3 input codes. If all concrete triplets map to the same
	// amino acid, 
	private static void populateAa(int i, int j, int k) {
		char char0 = ntChars.charAt(i);
		char char1 = ntChars.charAt(j);
		char char2 = ntChars.charAt(k);
		char[] possibleBases0 = ntCharToPossibleBases(char0);
		char[] possibleBases1 = ntCharToPossibleBases(char1);
		char[] possibleBases2 = ntCharToPossibleBases(char2);
		char[] triplet = new char[3];
		LinkedHashSet<String> possibleAAs = new LinkedHashSet<String>();
		for(char base0: possibleBases0) {
			triplet[0] = base0;
			for(char base1: possibleBases1) {
				triplet[1] = base1;
				for(char base2: possibleBases2) {
					triplet[2] = base2;
					String possibleAA = standardCodonTableMap.get(new String(triplet));
					possibleAAs.add(possibleAA);
				}
			}
		}
		if(possibleAAs.size() == 1) {
			standardCodonTableTripletToAa[i][j][k] = possibleAAs.iterator().next().charAt(0);
		} else {
			standardCodonTableTripletToAa[i][j][k] = 'X';
		}
	}
	
	// populate 3d array for all possible combinations of 3 IUPAC codes.
	static {
		GlueLogger.getGlueLogger().finest("Initialising fast amino acid translation subsystem.");
		for(int i = 0; i < 16; i++) {
			for(int j = 0; j < 16; j++) {
				for(int k = 0; k < 16; k++) {
					populateAa(i, j, k);
				}
			}
		}
		GlueLogger.getGlueLogger().finest("Initialisation complete.");
	}
	
	
	/**
	 * 
	 *	Nucleic Acid Code	Meaning								Mnemonic
	 * ---------------------------------------------------------------------------------
	 *	A					A							 		Adenine
	 *	C					C									Cytosine
	 *	G					G									Guanine
	 *	T					T									Thymine
	 *	U					U									Uracil
	 *	R					A or G								puRine
	 *	Y					C, T or U							pYrimidines
	 *	K					G, T or U							bases which are Ketones
	 *	M					A or C								bases with aMino groups
	 *	S					C or G								Strong interaction
	 *	W					A, T or U							Weak interaction
	 *	B					not A (i.e. C, G, T or U)			B comes after A
	 *	D					not C (i.e. A, G, T or U)			D comes after C
	 *	H					not G (i.e., A, C, T or U)			H comes after G
	 *	V					neither T nor U (i.e. A, C or G)	V comes after U
	 *	N					A C G T U							Nucleic acid
	 *	-					gap of indeterminate length	
 	 *
	 */
	
	// internally, each nucleotide code is represented as an integer between 0 and 16.

	private static int ntCharTo16BitInteger(char ntChar) {
		switch (ntChar) {
		case 'A':
			return 0;
		case 'C':
			return 1;
		case 'G':
			return 2;
		case 'T':
			return 3;
		case 'U':
			return 4;
		case 'R':
			return 5;
		case 'Y':
			return 6;
		case 'K':
			return 7;
		case 'M':
			return 8;
		case 'S':
			return 9;
		case 'W':
			return 10;
		case 'B':
			return 11;
		case 'D':
			return 12;
		case 'H':
			return 13;
		case 'V':
			return 14;
		case 'N':
			return 15;
		default:
			throw new TranslationException(Code.UNKNOWN_NUCLEOTIDE_CHAR, Character.toString(ntChar));
		}
	}

	
	
	// this method captures the ambiguities.
	private static char[] ntCharToPossibleBases(char ntChar) {
		switch (ntChar) {
		case 'A':
			return new char[]{'A'};
		case 'C':
			return new char[]{'C'};
		case 'G':
			return new char[]{'G'};
		case 'T':
			return new char[]{'T'};
		case 'U':
			return new char[]{'T'};
		case 'R':
			return new char[]{'A', 'G'};
		case 'Y':
			return new char[]{'C', 'T'};
		case 'K':
			return new char[]{'G', 'T'};
		case 'M':
			return new char[]{'A', 'C'};
		case 'S':
			return new char[]{'C', 'G'};
		case 'W':
			return new char[]{'A', 'T'};
		case 'B':
			return new char[]{'C', 'G', 'T'};
		case 'D':
			return new char[]{'A', 'G', 'T'};
		case 'H':
			return new char[]{'A', 'C', 'T'};
		case 'V':
			return new char[]{'A', 'C', 'G'};
		case 'N':
			return new char[]{'A', 'C', 'G', 'T'};
		default:
			throw new TranslationException(Code.UNKNOWN_NUCLEOTIDE_CHAR, Character.toString(ntChar));
		}
	}

	public static char translate(char[] bases) {
		return standardCodonTableTripletToAa
				[ntCharTo16BitInteger(bases[0])]
				[ntCharTo16BitInteger(bases[1])]
				[ntCharTo16BitInteger(bases[2])];
	}
	
	
	
}
