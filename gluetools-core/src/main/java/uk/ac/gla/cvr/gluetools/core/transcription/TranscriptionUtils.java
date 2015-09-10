package uk.ac.gla.cvr.gluetools.core.transcription;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.segments.AaReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.INtReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class TranscriptionUtils {

	public static TranscriptionFormat transcriptionFormatFromString(String formatString) {
		try {
			return TranscriptionFormat.valueOf(formatString);
		} catch(IllegalArgumentException iae) {
			throw new TranscriptionException(TranscriptionException.Code.UNKNOWN_TRANSCRIPTION_TYPE, formatString);
		}

	}

	/**
	 * Given a list of segments in NT coordinates, translate them to AA coordinates, given the NT location of codon 1.
	 * 
	 * The returned segments will include codons which are only partially covered by the input segments.
	 * 
	 */
	
	public static List<ReferenceSegment> translateToCodonCoordinates(int codon1Start, List <? extends IReferenceSegment> ntSegments) {
		return ntSegments.stream()
			.map(ntSegment -> new ReferenceSegment(
				getCodon(codon1Start, ntSegment.getRefStart()), 
				getCodon(codon1Start, ntSegment.getRefEnd())
				))
			.collect(Collectors.toList());
	}
	
	/**
	 * Given nucleotide segments and a nucleotide location specifying the nucleotide at which codon 1 starts,
	 * transcribe the nucleotides into amino-acid segments.
	 * 
	 * It is a precondition that the nucleotide segments do not cover any NT location prior to codon1Start.
	 */
	public static List<AaReferenceSegment> transcribeAminoAcids(int codon1Start, 
			List<? extends INtReferenceSegment> ntRefSegments) {
		List<AaReferenceSegment> aaReferenceSegments = new ArrayList<AaReferenceSegment>();
		// codon at which the current AA seg starts
		Integer aaSegStartCodon = null;
		// accumulated AAs for the current AA seg
		StringBuffer aaSegBuffer = null;
		// next codon to produce
		Integer aaSegNextCodon = null;
		// NTs for the next codon
		char[] currentCodonNts = new char[3];
		int[] currentCodonNtIndices = new int[3];
		
		for(INtReferenceSegment ntRefSegment: ntRefSegments) {
			int ntSegStartCodon = getCodon(codon1Start, ntRefSegment.getRefStart());
			int ntSegEndCodon = getCodon(codon1Start, ntRefSegment.getRefEnd());
			
			if(aaSegStartCodon != null // AA seg in progress  
					&& ntSegStartCodon > aaSegNextCodon) { // but NT seg starts after the end of the current AA seg.
				// Wrap up old AA seg
				String aminoAcids = aaSegBuffer.toString();
				aaReferenceSegments.add(newAaSegment(aaSegStartCodon, aminoAcids));
				aaSegStartCodon = null;
				aaSegNextCodon = null;
				aaSegBuffer = null;
				clearCodonNts(currentCodonNts, currentCodonNtIndices);
			} 
			
			// iterate over the codon locations covered by the ntRefSegment
			for(int ntSegCodon = ntSegStartCodon; ntSegCodon <= ntSegEndCodon; ntSegCodon++) {
				if(aaSegStartCodon != null && ntSegCodon < aaSegNextCodon) {
					continue; // can happen if the last AA was determined on fewer than 3 bases.
				}
				int startNT = getNt(codon1Start, ntSegCodon);
				// get the codon NTs from the segment
				int nextNT = populateCodonNtsFromNtSeg(startNT, currentCodonNts, currentCodonNtIndices, ntRefSegment);
				// attempt to transcribe
				char aa = transcribe(currentCodonNts, currentCodonNtIndices);
				if(aa == 0) { // failed or incomplete transcription.
					if(nextNT == startNT+3) { // failed transcription
						if(aaSegStartCodon != null) { // AA seg in progress
							// wrap up current AA seg
							aaReferenceSegments.add(newAaSegment(aaSegStartCodon, aaSegBuffer.toString()));
							aaSegStartCodon = null;
							aaSegNextCodon = null;
							aaSegBuffer = null;
							clearCodonNts(currentCodonNts, currentCodonNtIndices);
						}
					} 
				} else { // successful transcription
					if(aaSegStartCodon == null) { // no current AA seg in progress						
						// start new AA seg
						aaSegStartCodon = ntSegCodon;
						aaSegNextCodon = ntSegCodon;
						aaSegBuffer = new StringBuffer();
						clearCodonNts(currentCodonNts, currentCodonNtIndices);
					} 
					// add AA to segment.
					aaSegBuffer.append(aa);
					aaSegNextCodon ++;
					clearCodonNts(currentCodonNts, currentCodonNtIndices);
				}
			}
		}
		// wrap up any remaining aaSegment.
		if(aaSegStartCodon != null) {
			aaReferenceSegments.add(newAaSegment(aaSegStartCodon, aaSegBuffer.toString()));
		}
		return aaReferenceSegments;
	}


	private static AaReferenceSegment newAaSegment(Integer aaSegStartCodon,
			String aminoAcids) {
		return new AaReferenceSegment(aaSegStartCodon, 
				aaSegStartCodon+(aminoAcids.length()-1), aminoAcids);
	}
	
	private static int populateCodonNtsFromNtSeg(int startNt, char[] codonNts, int[] currentCodonNtIndices, INtReferenceSegment ntRefSegment) {
		int nextNT = startNt;
		for(int i = 0; i < 3; i++) {
			int refLocation = startNt+i;
			if(refLocation >= ntRefSegment.getRefStart() && refLocation <= ntRefSegment.getRefEnd()) {
				nextNT = refLocation+1;
				codonNts[i] = ntRefSegment.ntAtRefLocation(refLocation);
				currentCodonNtIndices[i] = ntRefSegment.ntIndexAtRefLoction(refLocation);
			}
		}
		return nextNT;
	}
	
	private static void clearCodonNts(char[] codonNts, int[] currentCodonNtIndices) {
		codonNts[0] = 0;
		codonNts[1] = 0;
		codonNts[2] = 0;
		currentCodonNtIndices[0] = 0;
		currentCodonNtIndices[1] = 0;
		currentCodonNtIndices[2] = 0;
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

	
	/**
	 * codonNTs is an array of 3 nt characters, some of which may be 0, which indicates a missing base.
	 * Returns the amino acid code if known, or 0 otherwise.
	 * 
	 */
	public static char transcribe(char[] codonNTs, int[] codonNtIndices) {
		char firstBase = codonNTs[0];
		char secondBase = codonNTs[1];
		char thirdBase = codonNTs[2];
		
		// check NTs are contiguous, otherwise return "unknown".
		int firstNtIndex = codonNtIndices[0];
		int secondNtIndex = codonNtIndices[1];
		int thirdNtIndex = codonNtIndices[2];

		if(firstBase != 0 && secondBase != 0 && thirdBase != 0) {
			if(secondNtIndex != firstNtIndex+1 ||
					thirdNtIndex != secondNtIndex+1) {
				return 0;
			}
		} else if(firstBase != 0 && secondBase != 0) {
			if(secondNtIndex != firstNtIndex+1) {
				return 0;
			}
		} else if(secondBase != 0 && thirdBase != 0) {
			if(thirdNtIndex != secondNtIndex+1) {
				return 0;
			}
		} 
		
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
	 * Example, if codon 1 starts at NT 3285, then NT 3287 is in codon 1, NT 3288 is in codon 2 etc.
	 */
	private static int getCodon(int codon1Start, int ntLocation) {
		return 1 + ((ntLocation - codon1Start) / 3);
	}
	
	/**
	 * Given a codon number, what nucleotide location does this codon start at.
	 * Example, if codon 1 starts at NT 3285, then codon 4 starts at 3294.
	 */
	private static int getNt(int codon1Start, int codon) {
		return ((codon-1) * 3)+codon1Start;
	}

}
