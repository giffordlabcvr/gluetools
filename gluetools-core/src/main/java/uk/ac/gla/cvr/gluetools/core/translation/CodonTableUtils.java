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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.bitmap.BitmapUtils;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;



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
	
	
	// 3-dimensional array mapping triplet of ambiguous NT (integer representation)
	// to AmbigNtTripletInfo instance
	private static TripletTranslationInfo[][][] ambigNtTripletToInfo = new TripletTranslationInfo[16][16][16];
	
	// return AmbigNtTripletInfo for specific triplet of ambiguous NTs (integer representation)
	private static TripletTranslationInfo computeAmbigNtTripletInfo(int ambigNt1, int ambigNt2, int ambigNt3) {
		// find the set of all possible concrete NT triplets for the ambiguous NT triplet
		// mapping each of these to AA, this gives the set of possible AAs for the ambiguous NT triplet
		// for each AA in turn
		//    - delete the concrete NT triplets which code for that AA from the concrete triplets set.
		//    - if the set is now empty then the AA is "definitely present".
		//      otherwise
		//    - recompute the ambiguous triplet for the remaining concrete triplets.
		//    - if this is different from the original ambiguous triplet, then the AA is "definitely present", 
		//      otherwise it is merely "possibly present".
		// Example 1
		// ambiguous NT triplet: YAY (Y = C/T)
		// concrete triplets set: CAC, CAT, TAC, TAT
		// AAs: Y (CAC, CAT), H (TAC, TAT)
		// Deleting CAC, CAT from the set gives TAC, TAT, equivalent to ambiguous triplet TAY, != YAY so Y is definite.
		// Deleting TAC, TAT from the set gives CAC, CAT, equivalent to ambiguous triplet CAY, != YAY so H is definite.
		// Example 2
		// ambiguous NT triplet: TSR (S = C/G, R = A/G)
		// concrete triplets set: TCA, TCG, TGA, TGG
		// AAs: S (TCA, TCG), * (TGA), W (TGG)
		// Deleting TCA, TCG from the set gives TGA, TGG, equivalent to ambiguous triplet TGR, != TSR so S is definite.
		// Deleting TGA from the set gives TCA, TCG, TGG, equivalent to ambiguous triplet TSR so * is merely possible.
		// Deleting TGG from the set gives TCA, TCG, TGA, equivalent to ambiguous triplet TSR so W is merely possible.
		boolean log = false;
		LinkedHashSet<Integer> possibleAas = new LinkedHashSet<Integer>();
		LinkedHashSet<Integer> triplets = new LinkedHashSet<Integer>();
		for(int concreteNt1: ResidueUtils.ambigNtToConcreteNts(ambigNt1)) {
			for(int concreteNt2: ResidueUtils.ambigNtToConcreteNts(ambigNt2)) {
				for(int concreteNt3: ResidueUtils.ambigNtToConcreteNts(ambigNt3)) {
					int aa = concreteNtTripletToAa[concreteNt1][concreteNt2][concreteNt3];
					possibleAas.add(aa);
					triplets.add(concreteNtTripletToInt(concreteNt1, concreteNt2, concreteNt3));
				}
			}
		}
		LinkedList<Integer> definiteAas = new LinkedList<Integer>();
		for(Integer possibleAa: possibleAas) {
			if(log) {
				System.out.println("possibleAa: "+Character.toString(ResidueUtils.intToAa(possibleAa)));
			}
			LinkedList<Integer> deletedTriplets = new LinkedList<Integer>();
			@SuppressWarnings("unchecked")
			List<int[]> codingTriplets = (List<int[]>) aaToConcreteNtTriplets[possibleAa];
			for(int[] codingTriplet: codingTriplets) {
				int codingTripletInt = concreteNtTripletToInt(codingTriplet[0], codingTriplet[1], codingTriplet[2]);
				if(triplets.remove(codingTripletInt)) {
					if(log) {
						StringBuffer tripletStringBuf = new StringBuffer();
						for(int nt: codingTriplet) { tripletStringBuf.append(ResidueUtils.intToConcreteNt(nt)); }
						System.out.println("removed triplet: "+tripletStringBuf.toString());
					}
					deletedTriplets.add(codingTripletInt);
				}
			}
			if(triplets.isEmpty()) {
				definiteAas.add(possibleAa);
			} else {
				int[] pos1ConcreteNts = new int[triplets.size()];
				int[] pos2ConcreteNts = new int[triplets.size()];
				int[] pos3ConcreteNts = new int[triplets.size()];
				int i = 0;
				for(int remainingTripletInt: triplets) {
					int[] remainingTriplet = intToConcreteNtTriplet(remainingTripletInt);
					if(log) {
						StringBuffer tripletStringBuf = new StringBuffer();
						for(int nt: remainingTriplet) { tripletStringBuf.append(ResidueUtils.intToConcreteNt(nt)); }
						System.out.println("remaining triplet: "+tripletStringBuf.toString());
					}
					pos1ConcreteNts[i] = remainingTriplet[0];
					pos2ConcreteNts[i] = remainingTriplet[1];
					pos3ConcreteNts[i] = remainingTriplet[2];
					i++;
				}
				if(log) {
					System.out.println("pos1ConcreteNts: "+Arrays.stream(pos1ConcreteNts).boxed().collect(Collectors.toList()));
					System.out.println("pos2ConcreteNts: "+Arrays.stream(pos2ConcreteNts).boxed().collect(Collectors.toList()));
					System.out.println("pos3ConcreteNts: "+Arrays.stream(pos3ConcreteNts).boxed().collect(Collectors.toList()));
				}
				int pos1Bitmap = BitmapUtils.intsToIntBitmap(pos1ConcreteNts);
				int pos2Bitmap = BitmapUtils.intsToIntBitmap(pos2ConcreteNts);				
				int pos3Bitmap = BitmapUtils.intsToIntBitmap(pos3ConcreteNts);
				if(log) {
					System.out.println("pos1Bitmap: "+Integer.toBinaryString(pos1Bitmap));
					System.out.println("pos2Bitmap: "+Integer.toBinaryString(pos2Bitmap));
					System.out.println("pos3Bitmap: "+Integer.toBinaryString(pos3Bitmap));
				}
				int pos1AmbigNt = ResidueUtils.concreteNtsBitmapToAmbigNt(pos1Bitmap);
				int pos2AmbigNt = ResidueUtils.concreteNtsBitmapToAmbigNt(pos2Bitmap);
				int pos3AmbigNt = ResidueUtils.concreteNtsBitmapToAmbigNt(pos3Bitmap);
				if(log) {
					System.out.println("pos1AmbigNt: "+ResidueUtils.intToAmbigNt(pos1AmbigNt));
					System.out.println("pos2AmbigNt: "+ResidueUtils.intToAmbigNt(pos2AmbigNt));
					System.out.println("pos3AmbigNt: "+ResidueUtils.intToAmbigNt(pos3AmbigNt));
				}
				if(pos1AmbigNt != ambigNt1 || pos2AmbigNt != ambigNt2 || pos3AmbigNt != ambigNt3) {
					definiteAas.add(possibleAa);
				}
			}
			triplets.addAll(deletedTriplets);
		}
		List<Character> definiteAaChars = definiteAas.stream()
				.map(intAa -> ResidueUtils.intToAa(intAa))
				.collect(Collectors.toList());
		List<Character> possibleAaChars = possibleAas.stream()
				.map(intAa -> ResidueUtils.intToAa(intAa))
				.collect(Collectors.toList());
		List<Character> tripletNts = new ArrayList<Character>(3);
		tripletNts.add(ResidueUtils.intToAmbigNt(ambigNt1));
		tripletNts.add(ResidueUtils.intToAmbigNt(ambigNt2));
		tripletNts.add(ResidueUtils.intToAmbigNt(ambigNt3));
		return new TripletTranslationInfo(tripletNts, definiteAaChars, possibleAaChars);
	}
	
	// populate ambigNtTripletToInfo for all possible ambiguous NT triplets.
	static {
		GlueLogger.getGlueLogger().finest("Initialising amino acid translation subsystem.");
		for(int intAmbigNt1 = 0; intAmbigNt1 < 16; intAmbigNt1++) {
			for(int intAmbigNt2 = 0; intAmbigNt2 < 16; intAmbigNt2++) {
				for(int intAmbigNt3 = 0; intAmbigNt3 < 16; intAmbigNt3++) {
					ambigNtTripletToInfo[intAmbigNt1][intAmbigNt2][intAmbigNt3] = 
							computeAmbigNtTripletInfo(intAmbigNt1, intAmbigNt2, intAmbigNt3);
				}
			}
		}
		GlueLogger.getGlueLogger().finest("Initialisation complete.");
	}
	
	

	public static char translateToSingleChar(char[] bases) {
		return getTranslationInfo(bases).getSingleCharTranslation();
	}

	public static TripletTranslationInfo getTranslationInfo(char[] bases) {
		return ambigNtTripletToInfo
				[ResidueUtils.ambigNtToInt(bases[0])]
				[ResidueUtils.ambigNtToInt(bases[1])]
				[ResidueUtils.ambigNtToInt(bases[2])];
	}
	
	// Given a triplet of concrete NTs in integer representation, 
	// return an int (0-63) which combines them in bitwise form.
	private static int concreteNtTripletToInt(int intConcreteNt1, int intConcreteNt2, int intConcreteNt3) {
		return (intConcreteNt1 << 4) | (intConcreteNt2 << 2) | (intConcreteNt3);
	}
	
	// Given an int (0-63) which combines 3 concrete NTs in bitwise form.
	// return an array with separate integers, 
	private static int[] intToConcreteNtTriplet(int concreteNtTriplet) {
		return new int[] {
							(concreteNtTriplet >> 4) & 3, 
							(concreteNtTriplet >> 2) & 3, 
							(concreteNtTriplet) & 3};
	}
	
	private static String charListToString(List<Character> charList) {
		char[] charArray = new char[charList.size()];
		for(int i = 0; i < charList.size(); i++) {
			charArray[i] = charList.get(i).charValue();
		}
		return new String(charArray);
	}
	
	/**
	 * Encapsulates possible / definite translations, based on a nucleotide triplet
	 * which may contain ambiguity codes.
	 *
	 */
	public static class TripletTranslationInfo {
		private List<Character> tripletNts;
		private List<Character> definiteAminoAcids;
		private List<Character> possibleAminoAcids;
		private String definiteAasString;
		private String possibleAasString;
		private String tripletNtsString;
		private char singleCharTranslation;
		private String singleCharTranslationString;
		
		private TripletTranslationInfo(List<Character> tripletNts, List<Character> definiteAminoAcids,
				List<Character> possibleAminoAcids) {
			super();
			this.tripletNts = tripletNts;
			this.tripletNtsString = charListToString(tripletNts);
			this.definiteAminoAcids = definiteAminoAcids;
			this.definiteAasString = charListToString(definiteAminoAcids);
			this.possibleAminoAcids = possibleAminoAcids;
			this.possibleAasString = charListToString(possibleAminoAcids);
			if(definiteAminoAcids.size() == 1) {
				this.singleCharTranslation = definiteAminoAcids.get(0);
			} else {
				this.singleCharTranslation = 'X';
			}
			this.singleCharTranslationString = new String(new char[]{this.singleCharTranslation});
		}

		public char getSingleCharTranslation() {
			return singleCharTranslation;
		}

		public String getSingleCharTranslationString() {
			return singleCharTranslationString;
		}

		public List<Character> getDefiniteAminoAcids() {
			return definiteAminoAcids;
		}

		public List<Character> getPossibleAminoAcids() {
			return possibleAminoAcids;
		}

		public String getDefiniteAasString() {
			return definiteAasString;
		}

		public String getPossibleAasString() {
			return possibleAasString;
		}

		public List<Character> getTripletNts() {
			return tripletNts;
		}

		public String getTripletNtsString() {
			return tripletNtsString;
		}
		
		
		
	}
	
	public static void main(String[] args) {
		System.out.println("ATG: "+getTranslationInfo("ATG".toCharArray()));
		System.out.println("CAY: "+getTranslationInfo("CAY".toCharArray()));
		System.out.println("TAY: "+getTranslationInfo("YAY".toCharArray()));
		System.out.println("TSR: "+getTranslationInfo("TSR".toCharArray()));
		System.out.println("NNN: "+getTranslationInfo("NNN".toCharArray()));
	}
	
}
