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

import gnu.trove.map.TCharCharMap;
import gnu.trove.map.hash.TCharCharHashMap;
import uk.ac.gla.cvr.gluetools.core.bitmap.BitmapUtils;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationException.Code;

public class ResidueUtils {

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
	 */
	
	public static String ALL_AMBIG_NTS = "ACGTURYKMSWBDHVN";

	public static final int
		AMBIG_NT_A = 0,
		AMBIG_NT_C = 1,
		AMBIG_NT_G = 2,
		AMBIG_NT_T = 3,
		AMBIG_NT_U = 4,
		AMBIG_NT_R = 5,
		AMBIG_NT_Y = 6,
		AMBIG_NT_K = 7,
		AMBIG_NT_M = 8,
		AMBIG_NT_S = 9,
		AMBIG_NT_W = 10,
		AMBIG_NT_B = 11,
		AMBIG_NT_D = 12,
		AMBIG_NT_H = 13,
		AMBIG_NT_V = 14,
		AMBIG_NT_N = 15;

	public static final int AMBIG_NT_NUM_VALUES = 16;
	
	private static String ALL_CONCRETE_NTS = "ACGT";

	public static final int
		CONCRETE_NT_A = 0,
		CONCRETE_NT_C = 1,
		CONCRETE_NT_G = 2,
		CONCRETE_NT_T = 3;

	public static final int CONCRETE_NT_NUM_VALUES = 4;

	public static String ALL_AAS = "ACDEFGHIKLMNPQRSTVWY*";

	public static final int
		AA_A = 0,	//	Ala	Alanine
		AA_C = 1,	//	Cys	Cysteine
		AA_D = 2,	//	Asp	Aspartic Acid
		AA_E = 3,	//	Glu	Glutamic Acid
		AA_F = 4,	//	Phe	Phenylalanine
		AA_G = 5,	//	Gly	Glycine
		AA_H = 6,	//	His	Histidine
		AA_I = 7,	//	Ile	Isoleucine
		AA_K = 8,	//	Lys	Lysine
		AA_L = 9,	//	Leu	Leucine
		AA_M = 10,	//	Met	Methionine
		AA_N = 11,	//	Asn	Asparagine
		AA_P = 12,	//	Pro	Proline
		AA_Q = 13,	//	Gln	Glutamine
		AA_R = 14,	//	Arg	Arginine
		AA_S = 15,	//	Ser	Serine
		AA_T = 16,	//	Thr	Threonine
		AA_V = 17,	//	Val	Valine
		AA_W = 18,	//	Trp	Tryptophan
		AA_Y = 19,	//	Tyr	Tyrosine
		AA_STOP = 20;	//	Stop codon

	public static final int AA_NUM_VALUES = 21;

	public static int aaShortNameToInt(String shortName) {
		switch(shortName) {
		case "Ala":
		    return AA_A;
		case "Cys":
		    return AA_C;
		case "Asp":
		    return AA_D;
		case "Glu":
		    return AA_E;
		case "Phe":
		    return AA_F;
		case "Gly":
		    return AA_G;
		case "His":
		    return AA_H;
		case "Ile":
		    return AA_I;
		case "Lys":
		    return AA_K;
		case "Leu":
		    return AA_L;
		case "Met":
		    return AA_M;
		case "Asn":
		    return AA_N;
		case "Pro":
		    return AA_P;
		case "Gln":
		    return AA_Q;
		case "Arg":
		    return AA_R;
		case "Ser":
		    return AA_S;
		case "Thr":
		    return AA_T;
		case "Val":
		    return AA_V;
		case "Trp":
		    return AA_W;
		case "Tyr":
		    return AA_Y;
		default:
			throw new ResidueUtilsException(ResidueUtilsException.Code.UNKNOWN_AMINO_ACID_SHORT_NAME, shortName);
		}
	}

	public static String aaIntToShortName(int aaInt) {
		switch(aaInt) {
		case AA_A:
		    return "Ala";
		case AA_C:
		    return "Cys";
		case AA_D:
		    return "Asp";
		case AA_E:
		    return "Glu";
		case AA_F:
		    return "Phe";
		case AA_G:
		    return "Gly";
		case AA_H:
		    return "His";
		case AA_I:
		    return "Ile";
		case AA_K:
		    return "Lys";
		case AA_L:
		    return "Leu";
		case AA_M:
		    return "Met";
		case AA_N:
		    return "Asn";
		case AA_P:
		    return "Pro";
		case AA_Q:
		    return "Gln";
		case AA_R:
		    return "Arg";
		case AA_S:
		    return "Ser";
		case AA_T:
		    return "Thr";
		case AA_V:
		    return "Val";
		case AA_W:
		    return "Trp";
		case AA_Y:
		    return "Tyr";
		default:
			throw new ResidueUtilsException(ResidueUtilsException.Code.AA_INT_DOES_NOT_HAVE_SHORT_NAME, aaInt);
		}
	}

	
	
	public static char intToAmbigNt(int intAmbigNt) {
		return ALL_AMBIG_NTS.charAt(intAmbigNt);
	}
	
	// map ambiguous NT code to an integer between 0 and 15.
	public static int ambigNtToInt(char ambigNt) {
		switch (ambigNt) {
		case 'A':
			return AMBIG_NT_A;
		case 'C':
			return AMBIG_NT_C;
		case 'G':
			return AMBIG_NT_G;
		case 'T':
			return AMBIG_NT_T;
		case 'U':
			return AMBIG_NT_U;
		case 'R':
			return AMBIG_NT_R;
		case 'Y':
			return AMBIG_NT_Y;
		case 'K':
			return AMBIG_NT_K;
		case 'M':
			return AMBIG_NT_M;
		case 'S':
			return AMBIG_NT_S;
		case 'W':
			return AMBIG_NT_W;
		case 'B':
			return AMBIG_NT_B;
		case 'D':
			return AMBIG_NT_D;
		case 'H':
			return AMBIG_NT_H;
		case 'V':
			return AMBIG_NT_V;
		case 'N':
			return AMBIG_NT_N;
		default:
			throw new TranslationException(Code.UNKNOWN_NUCLEOTIDE_CHAR, Character.toString(ambigNt));
		}
	}

	public static char intToConcreteNt(int intConcreteNt) {
		return ALL_CONCRETE_NTS.charAt(intConcreteNt);
	}
	
	public static int concreteNtToInt(char concreteNt) {
		switch (concreteNt) {
		case 'A':
			return CONCRETE_NT_A;
		case 'C':
			return CONCRETE_NT_C;
		case 'G':
			return CONCRETE_NT_G;
		case 'T':
			return CONCRETE_NT_T;
		default:
			throw new TranslationException(Code.UNKNOWN_CONCRETE_NUCLEOTIDE_CHAR, Character.toString(concreteNt));
		}
	}

	public static char intToAa(int intAa) {
		return ALL_AAS.charAt(intAa);
	}

	public static int aaToInt(char aa) {
		switch (aa) {
		case 'A':
			return AA_A;
		case 'C':
			return AA_C;
		case 'D':
			return AA_D;
		case 'E':
			return AA_E;
		case 'F':
			return AA_F;
		case 'G':
			return AA_G;
		case 'H':
			return AA_H;
		case 'I':
			return AA_I;
		case 'K':
			return AA_K;
		case 'L':
			return AA_L;
		case 'M':
			return AA_M;
		case 'N':
			return AA_N;
		case 'P':
			return AA_P;
		case 'Q':
			return AA_Q;
		case 'R':
			return AA_R;
		case 'S':
			return AA_S;
		case 'T':
			return AA_T;
		case 'V':
			return AA_V;
		case 'W':
			return AA_W;
		case 'Y':
			return AA_Y;
		case '*': 
			return AA_STOP;	
		default:
			throw new TranslationException(Code.UNKNOWN_AMINO_ACID_CHAR, Character.toString(aa));
		}
	}
	
	
	private static char[][] ntComplements = {
	    {'A',	'T'},
	    {'C',	'G'},
	    {'G',	'C'},
	    {'T',	'A'},
	    {'U',	'A'},
	    {'W',	'W'},
	    {'S',	'S'},
	    {'M',	'K'},
	    {'K',	'M'},
	    {'R',	'Y'},
	    {'Y',	'R'},
	    {'B',	'V'},
	    {'D',	'H'},
	    {'H',	'D'},
	    {'V',	'B'},
	    {'N',	'N'},
	    {'Z',	'Z'}
	};
	
	private static TCharCharMap ambigNtCharToComplement = new TCharCharHashMap();
	
	static {
		for(int i = 0; i < ntComplements.length; i++) {
			ambigNtCharToComplement.put(ntComplements[i][0], ntComplements[i][1]);
		}
	}
	
	// map ambiguous NT to an array of the underlying concrete NTs
	private static int[][] ambigNtToConcreteNts = new int[16][];
	// map a bitmap of concrete NTs to an ambiguous NT
	private static int[] concreteNtsBitmapToAmbigNt = new int[16];

	// maps combination of ambig NT and concrete NT to
	// probability of concrete NT given ambig NT.
	private static double ambigNtToConcreteNtProb[][] = 
			new double[AMBIG_NT_NUM_VALUES][CONCRETE_NT_NUM_VALUES];

	// populate ambigNtToConcreteNtsBitmap
	static {
		ambigNtToConcreteNts[AMBIG_NT_A] =
			new int[]{CONCRETE_NT_A};
		ambigNtToConcreteNts[AMBIG_NT_C] =
			new int[]{CONCRETE_NT_C};
		ambigNtToConcreteNts[AMBIG_NT_G] =
			new int[]{CONCRETE_NT_G};
		ambigNtToConcreteNts[AMBIG_NT_T] =
			new int[]{CONCRETE_NT_T};
		ambigNtToConcreteNts[AMBIG_NT_U] =
			new int[]{CONCRETE_NT_T};
		ambigNtToConcreteNts[AMBIG_NT_R] =
			new int[]{CONCRETE_NT_A, CONCRETE_NT_G};
		ambigNtToConcreteNts[AMBIG_NT_Y] =
			new int[]{CONCRETE_NT_C, CONCRETE_NT_T};
		ambigNtToConcreteNts[AMBIG_NT_K] =
			new int[]{CONCRETE_NT_G, CONCRETE_NT_T};
		ambigNtToConcreteNts[AMBIG_NT_M] =
			new int[]{CONCRETE_NT_A, CONCRETE_NT_C};
		ambigNtToConcreteNts[AMBIG_NT_S] =
			new int[]{CONCRETE_NT_C, CONCRETE_NT_G};
		ambigNtToConcreteNts[AMBIG_NT_W] =
			new int[]{CONCRETE_NT_A, CONCRETE_NT_T};
		ambigNtToConcreteNts[AMBIG_NT_B] =
			new int[]{CONCRETE_NT_C, CONCRETE_NT_G, CONCRETE_NT_T};
		ambigNtToConcreteNts[AMBIG_NT_D] =
			new int[]{CONCRETE_NT_A, CONCRETE_NT_G, CONCRETE_NT_T};
		ambigNtToConcreteNts[AMBIG_NT_H] =
			new int[]{CONCRETE_NT_A, CONCRETE_NT_C, CONCRETE_NT_T};
		ambigNtToConcreteNts[AMBIG_NT_V] =
			new int[]{CONCRETE_NT_A, CONCRETE_NT_C, CONCRETE_NT_G};
		ambigNtToConcreteNts[AMBIG_NT_N] =
			new int[]{CONCRETE_NT_A, CONCRETE_NT_C, CONCRETE_NT_G, CONCRETE_NT_T};

		// populate concreteNtsBitmapToAmbigNt
		for(int ambigNt = 0 ; ambigNt < 16; ambigNt++) {
			int[] concreteNts = ambigNtToConcreteNts[ambigNt];
			if(ambigNt != AMBIG_NT_U) { // don't overwrite mapping for this concrete NT combination.
				int concreteNtsBitmap = BitmapUtils.intsToIntBitmap(concreteNts);
				concreteNtsBitmapToAmbigNt[concreteNtsBitmap] = ambigNt;
				//System.out.println("concreteNts: "+Arrays.stream(concreteNts).boxed().collect(Collectors.toList()));
				//System.out.println("concreteNtsBitmap: "+Integer.toBinaryString(concreteNtsBitmap));
				//System.out.println("concreteNtsBitmapToAmbigNt: "+intToAmbigNt(ambigNt));
			}
			for(int concreteNt: concreteNts) {
				ambigNtToConcreteNtProb[ambigNt][concreteNt] = 1 / (double) concreteNts.length;
			}
		}
	}
	
	public static int[] ambigNtToConcreteNts(int ambigNt) {
		return ambigNtToConcreteNts[ambigNt];
	}
	
	public static double ambigNtToConcreteNtProb(int ambigNt, int concreteNt) {
		return ambigNtToConcreteNtProb[ambigNt][concreteNt];
	}
	
	public static int concreteNtsToAmbigNt(int[] concreteNts) {
		int concreteNtsBitmap = BitmapUtils.intsToIntBitmap(concreteNts);
		return concreteNtsBitmapToAmbigNt(concreteNtsBitmap);
	}

	public static int concreteNtsBitmapToAmbigNt(int concreteNtsBitmap) {
		return concreteNtsBitmapToAmbigNt[concreteNtsBitmap];
	}

	public static char complementAmbigNtChar(char ambigNtChar) {
		char upperCaseANC = ambigNtChar;
		boolean isLowerCase = false;
		if(Character.isLowerCase(ambigNtChar)) {
			upperCaseANC = Character.toUpperCase(ambigNtChar);
			isLowerCase = true;
		}
		char complement;
		if(ambigNtCharToComplement.containsKey(upperCaseANC)) {
			complement = ambigNtCharToComplement.get(upperCaseANC);
		} else {
			complement = upperCaseANC;
		}
		if(isLowerCase) {
			complement = Character.toLowerCase(complement);
		}
		return complement;
	}
	
}
