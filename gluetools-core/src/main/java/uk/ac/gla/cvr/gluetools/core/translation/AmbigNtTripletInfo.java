package uk.ac.gla.cvr.gluetools.core.translation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.bitmap.BitmapUtils;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

/**
 * Encapsulates possible / definite translations, based on a nucleotide triplet
 * which may contain ambiguity codes.
 *
 */
public class AmbigNtTripletInfo {
	private List<Character> tripletNts;
	private List<Character> definiteAminoAcids;
	private List<Character> possibleAminoAcids;
	// maps integer AA value to a double. The fraction represents, of those
	// concrete NT triplets which are consistent with this 
	// ambiguous NT triplet, what fraction translate to
	// the AA residue? 0.0 for non-possible AAs.
	private double[] aaIntToTripletsFraction;
	private String definiteAasString;
	private String possibleAasString;
	private String tripletNtsString;
	private char singleCharTranslation;
	private String singleCharTranslationString;
	private List<String> concreteNtTripletStrings;
	
	AmbigNtTripletInfo(List<Character> tripletNts, List<Character> definiteAminoAcids,
			List<Character> possibleAminoAcids, List<Double> possibleAminoAcidTripletsFraction, 
			Set<Integer> concreteNtTripletBitmaps) {
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
		this.aaIntToTripletsFraction = new double[ResidueUtils.AA_NUM_VALUES];
		for(int i = 0; i < possibleAminoAcids.size(); i++) {
			char possibleAa = possibleAminoAcids.get(i);
			this.aaIntToTripletsFraction[ResidueUtils.aaToInt(possibleAa)] = possibleAminoAcidTripletsFraction.get(i);
		}
		this.concreteNtTripletStrings = new ArrayList<String>();
		for(Integer concreteNtTripletBitmap: concreteNtTripletBitmaps) {
			int[] ntInts = intToConcreteNtTriplet(concreteNtTripletBitmap);
			StringBuffer ntTripletBuf = new StringBuffer(3);
			for(int ntInt: ntInts) {
				ntTripletBuf.append(ResidueUtils.intToConcreteNt(ntInt));
			}
			this.concreteNtTripletStrings.add(ntTripletBuf.toString());
		}
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

	public boolean isPossibleAa(char aa) {
		return getPossibleAaTripletsFraction(aa) > 0.0;
	}
	
	public double getPossibleAaTripletsFraction(char aa) {
		return aaIntToTripletsFraction[ResidueUtils.aaToInt(aa)];
		
	}
	
	
	@Override
	public String toString() {
		StringBuffer possibleAasBuf = new StringBuffer();
		possibleAasBuf.append("{");
		for(int i = 0; i < possibleAminoAcids.size(); i++) {
			if(i > 0) { possibleAasBuf.append(", "); }
			Character aa = possibleAminoAcids.get(i);
			possibleAasBuf.append(aa.toString());
			possibleAasBuf.append("=");
			possibleAasBuf.append(Double.toString(getPossibleAaTripletsFraction(aa)));
		}
		possibleAasBuf.append("}");
		return "AmbigNtTripletInfo ["+
				"concreteNtTriplets="+ concreteNtTripletStrings + ", "+
				"singleCharAa="+ singleCharTranslation + ", "+
				"definiteAas="+ definiteAasString + ", "+
				"possibleAas="+ possibleAasBuf.toString()+
				"]";
	}



	// 3-dimensional array mapping triplet of ambiguous NT (integer representation)
	// to AmbigNtTripletInfo instance
	private static AmbigNtTripletInfo[][][] ambigNtTripletToInfo = new AmbigNtTripletInfo[16][16][16];
	
	// return AmbigNtTripletInfo for specific triplet of ambiguous NTs (integer representation)
	private static AmbigNtTripletInfo computeAmbigNtTripletInfo(int ambigNt1, int ambigNt2, int ambigNt3) {
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
					int aa = CodonTableUtils.concreteNtTripletToAa(concreteNt1,concreteNt2,concreteNt3);
					possibleAas.add(aa);
					triplets.add(concreteNtTripletToInt(concreteNt1, concreteNt2, concreteNt3));
				}
			}
		}
		int totalNumConcreteTriplets = triplets.size();
		LinkedList<Double> possibleAaTripletFractions = new LinkedList<Double>();
		LinkedList<Integer> definiteAas = new LinkedList<Integer>();
		for(Integer possibleAa: possibleAas) {
			if(log) {
				System.out.println("possibleAa: "+Character.toString(ResidueUtils.intToAa(possibleAa)));
			}
			LinkedList<Integer> deletedTriplets = new LinkedList<Integer>();
			@SuppressWarnings("unchecked")
			List<int[]> codingTriplets = (List<int[]>) CodonTableUtils.aaToConcreteNtTriplets(possibleAa);
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
			// number of deleted triplets, which both code for the possible AA and are consistent
			// with the ambiguous NT triplet.
			int numDeletedTriplets = deletedTriplets.size();
			double tripletsFraction = ((double) numDeletedTriplets) / (double) totalNumConcreteTriplets;
			possibleAaTripletFractions.add(tripletsFraction);
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
		return new AmbigNtTripletInfo(tripletNts, definiteAaChars, possibleAaChars, possibleAaTripletFractions, triplets);
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

	public static AmbigNtTripletInfo getTranslationInfo(char[] bases) {
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
	
	static String charListToString(List<Character> charList) {
		char[] charArray = new char[charList.size()];
		for(int i = 0; i < charList.size(); i++) {
			charArray[i] = charList.get(i).charValue();
		}
		return new String(charArray);
	}
	
	public static void main(String[] args) {
		System.out.println("ATG: "+getTranslationInfo("ATG".toCharArray()));
		System.out.println("CAY: "+getTranslationInfo("CAY".toCharArray()));
		System.out.println("YAC: "+getTranslationInfo("YAC".toCharArray()));
		System.out.println("TAY: "+getTranslationInfo("YAY".toCharArray()));
		System.out.println("TSR: "+getTranslationInfo("TSR".toCharArray()));
		System.out.println("NNN: "+getTranslationInfo("NNN".toCharArray()));
	}




	
}