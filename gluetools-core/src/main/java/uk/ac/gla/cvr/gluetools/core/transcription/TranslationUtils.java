package uk.ac.gla.cvr.gluetools.core.transcription;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class TranslationUtils {

	public static TranslationFormat transcriptionFormatFromString(String formatString) {
		try {
			return TranslationFormat.valueOf(formatString);
		} catch(IllegalArgumentException iae) {
			throw new TranslationException(TranslationException.Code.UNKNOWN_TRANSLATION_TYPE, formatString);
		}

	}

	/**
	 * Given a list of segments in NT coordinates, translate them to AA coordinates, given the NT location of codon 1.
	 * 
	 * The returned segments will include codons which are only partially covered by the input segments.
	 * 
	 * However, each codon coordinate will be covered by at most one segment in the returned list.
	 * 
	 */
	
	public static List<ReferenceSegment> translateToCodonCoordinates(int codon1Start, List <? extends IReferenceSegment> ntSegments) {
		List<ReferenceSegment> resultList = new ArrayList<ReferenceSegment>();
		ReferenceSegment lastRefSeg = null;
		for(IReferenceSegment ntSegment: ntSegments) {
			int segmentCodonStart = getCodon(codon1Start, ntSegment.getRefStart());
			int segmentCodonEnd = getCodon(codon1Start, ntSegment.getRefEnd());
			ReferenceSegment newRefSeg = null;
			if(lastRefSeg == null) {
				newRefSeg = new ReferenceSegment(segmentCodonStart, segmentCodonEnd);
			} else if(segmentCodonStart > lastRefSeg.getRefEnd()) {
				newRefSeg = new ReferenceSegment(segmentCodonStart, segmentCodonEnd);
			} else if(segmentCodonEnd > lastRefSeg.getRefEnd()) {
				newRefSeg = new ReferenceSegment(lastRefSeg.getRefEnd()+1, segmentCodonEnd);
			}
			if(newRefSeg != null) {
				resultList.add(newRefSeg);
				lastRefSeg = newRefSeg;
			}
		}
		return resultList;
	}

	/**
	 * Translate nucleotides to amino acids.
	 * 
	 * If requireMethionineAtStart == true and
	 * the first 3 nucleotides do not produce 'M', translation stops and the empty string is returned.
	 * If stopAtHyphen == true and there is a gap of indeterminate length '-' in the nucleotides, translation stops at that gap, 
	 * and includes any AAs found before the gap.
	 * If any NTs are encountered which are definitely a stop codon, translation stops there and includes 
	 * the stop codon.
	 * If translateBeyondPossibleStopCodon == false and any NTs are encountered which *could* be a stop codon, 
	 * translation stops before the possible stop codon.
	 * 
	 */
	
	public static String translate(CharSequence nts,
			boolean requireMethionineAtStart, 
			boolean stopAtHyphen,
			boolean translateBeyondPossibleStopCodon) {
		StringBuffer aas = new StringBuffer();
		char[] codonNts = new char[3];
		boolean stopTranscribing = false;
		for(int ntIndex = 0; ntIndex < nts.length(); ntIndex +=3) {
			clearCodonNts(codonNts);
			for(int i = 0; i < 3; i++) {
				if(ntIndex+i < nts.length()) {
					char nt = nts.charAt(ntIndex+i);
					if(stopAtHyphen && nt == '-') {
						stopTranscribing = true;
						break;
					}
					codonNts[i] = nt;
				}
			}
			char nextAA = translate(codonNts);
			if(ntIndex == 0 && requireMethionineAtStart && nextAA != 'M') {
				stopTranscribing = true;
			} else if(nextAA == 0) {
				if(!translateBeyondPossibleStopCodon && couldBeStopCodon(codonNts)) {
					stopTranscribing = true;
				} else if(codonNts[0] != 0 && codonNts[1] != 0 && codonNts[2] != 0){
					aas.append('X'); // unknown AA
				}
			} else if(nextAA == '*') {
				aas.append(nextAA);
				stopTranscribing = true;
			} else {
				aas.append(nextAA);
			}
			if(stopTranscribing) {
				break;
			}
		}
		return aas.toString();
	}
	
	private static void clearCodonNts(char[] codonNts) {
		codonNts[0] = 0;
		codonNts[1] = 0;
		codonNts[2] = 0;
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
	 *	X					masked	
	 *	-					gap of indeterminate length	
 	 *
	 */

	public static boolean A(char nt) {
		return nt == 'A';
	}
	public static boolean C(char nt) {
		return nt == 'C';
	}
	public static boolean T_or_U(char nt) {
		return nt == 'T' || nt == 'U';
	}
	public static boolean G(char nt) {
		return nt == 'G';
	}
	public static boolean not_G(char nt) {
		return nt == 'A' || nt == 'C' || nt == 'T' || nt == 'U' || nt == 'Y' || nt == 'M' || nt == 'W' || nt == 'H';
	}
	public static boolean T_or_U_or_C(char nt) {
		return nt == 'T' || nt == 'U' || nt == 'C' || nt == 'Y';
	}
	public static boolean A_or_G(char nt) {
		return nt == 'A' || nt == 'G' || nt == 'R';
	}
	public static boolean A_or_C(char nt) {
		return nt == 'A' || nt == 'C' || nt == 'M';
	}

	private static boolean possible_A_or_G(char nt) {
		return nt == 0 || nt == 'A' || nt == 'G' || nt == 'R' || 
				nt == 'K' || nt == 'M' || nt == 'S' || nt == 'W' || nt == 'B' ||
				nt == 'D' || nt == 'H' || nt == 'V' || nt == 'N' || nt == 'X';
	}

	private static boolean possible_T_or_U(char nt) {
		return nt == 0 || nt == 'T' || nt == 'U' || nt == 'Y' || 
				nt == 'K' || nt == 'W' || nt == 'B' || nt == 'D' || nt == 'H' ||
				nt == 'N' || nt == 'X';
	}

	/**
	 * return true if the set of three characters could be a stop codon.
	 * @param codonNTs
	 */
	public static boolean couldBeStopCodon(char[] codonNTs) {
		char firstBase = codonNTs[0];
		char secondBase = codonNTs[1];
		char thirdBase = codonNTs[2];

		if(possible_T_or_U(firstBase) &&
				possible_A_or_G(secondBase) &&
				possible_A_or_G(thirdBase) &&
				!(G(secondBase) && G(thirdBase))) {
			return true;
		}
		return false;
	}
	
	// TODO produce ambiguous AA values: B, J or Z
	
	/**
	 * codonNTs is an array of 3 nt characters, some of which may be 0, which indicates a missing base.
	 * Returns the amino acid code if known, or 0 otherwise.
	 * 
	 */
	public static char translate(char[] codonNTs) {
		char firstBase = codonNTs[0];
		char secondBase = codonNTs[1];
		char thirdBase = codonNTs[2];
		
		if(T_or_U(firstBase)) {
			if(T_or_U(secondBase)) {
				if(T_or_U_or_C(thirdBase)) {
					return 'F'; // Phenylalanine
				}
				if(A_or_G(thirdBase)) {
					return 'L'; // Leucine
				}
			}
			if(C(secondBase)) {
				return 'S'; // Serine
			}
			if(A_or_G(secondBase)) {
				if(A(thirdBase)) {
					return '*'; // Stop codon (UAA or UGA)
				}
			}
			if(A(secondBase)) {
				if(T_or_U_or_C(thirdBase)) {
					return 'Y'; // Tyrosine
				}
				if(A_or_G(thirdBase)) {
					return '*'; // Stop codon (UAA or UAG)
				}
			}
			if(G(secondBase)) {
				if(T_or_U_or_C(thirdBase)) {
					return 'C'; // Cysteine
				}
				if(G(thirdBase)) {
					return 'W'; // Tryptophan
				}
			} 
		}
		if(C(firstBase)) {
			if(T_or_U(secondBase)) {
				return 'L'; // Leucine
			}
			if(C(secondBase)) {
				return 'P'; // Proline
			}
			if(A(secondBase)) {
				if(T_or_U_or_C(thirdBase)) {
					return 'H'; // Histidine
				}
				if(A_or_G(thirdBase)) {
					return 'Q'; // Glutamine
				}
			}
			if(G(secondBase)) {
				return 'R'; // Arginine
			}
		}
		if(A(firstBase)) {
			if(T_or_U(secondBase)) {
				if(not_G(thirdBase)) {
					return 'I'; // Isoleucine
				} if(G(thirdBase)) {
					return 'M'; // Methionine
				}
			}
			if(C(secondBase)) {
				return 'T'; // Threonine
			}
			if(A(secondBase)) {
				if(T_or_U_or_C(thirdBase)) {
					return 'N'; // Asparagine
				}
				if(A_or_G(thirdBase)) {
					return 'K'; // Lysine
				}
			}
			if(G(secondBase)) {
				if(T_or_U_or_C(thirdBase)) {
					return 'S'; // Serine
				}
				if(A_or_G(thirdBase)) {
					return 'R'; // Arginine
				}
			}
		}
		if(G(firstBase)) {
			if(T_or_U(secondBase)) {
				return 'V'; // Valine
			}
			if(C(secondBase)) {
				return 'A'; // Alanine
			}
			if(A(secondBase)) {
				if(T_or_U_or_C(thirdBase)) {
					return 'D'; // Aspartic Acid
				}
				if(A_or_G(thirdBase)) {
					return 'E'; // Glutamic Acid
				}
			}
			if(G(secondBase)) {
				return 'G'; // Glycine
			}
		}
		// additional ambiguity cases		
		if(T_or_U_or_C(firstBase)) {
			if(T_or_U(secondBase)) {
				if(A_or_G(thirdBase)) {
					return 'L'; // Leucine
				}
			}
		}
		if(A_or_C(firstBase)) {
			if(G(secondBase)) {
				if(A_or_G(thirdBase)) {
					return 'R'; // Arginine
				}
			}
		}
		
		return 0;
	}
	
	


	/**
	 * Given a nucleotide number, which codon is it in.
	 * Example, if codon 1 starts at NT 3285, then NT 3287 is in codon 1, NT 3288 is in codon 2 etc.
	 */
	public static int getCodon(int codon1Start, int ntLocation) {
		return 1 + ((ntLocation - codon1Start) / 3);
	}

	/**
	 * is a nucleotide at the start of a codon.
	 * @param ntLocation
	 * @return
	 */
	public static boolean isAtStartOfCodon(int codon1Start, int ntLocation) {
		return (ntLocation - codon1Start) % 3 == 0;
	}

	/**
	 * is a nucleotide at the start of a codon.
	 * @param ntLocation
	 * @return
	 */
	public static boolean isAtEndOfCodon(int codon1Start, int ntLocation) {
		return (ntLocation - codon1Start) % 3 == 2;
	}

	
	/**
	 * Given a codon number, what nucleotide location does this codon start at.
	 * Example, if codon 1 starts at NT 3285, then codon 4 starts at 3294.
	 */
	public static int getNt(int codon1Start, int codon) {
		return ((codon-1) * 3)+codon1Start;
	}

}
