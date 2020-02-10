package uk.ac.gla.cvr.gluetools.core.translation;

public class GranthamDistanceCalculator {

	private static double aaIntToC[] = new double[ResidueUtils.AA_NUM_VALUES];
	private static double aaIntToP[] = new double[ResidueUtils.AA_NUM_VALUES];
	private static double aaIntToV[] = new double[ResidueUtils.AA_NUM_VALUES];

	private static double alpha = 1.833; 
	private static double beta = 0.1018;
	private static double gamma = 0.000399;
	
	// map two AA ints to their Grantham Distance
	private static double aaIntsToGrantham[][] = new double[ResidueUtils.AA_NUM_VALUES][ResidueUtils.AA_NUM_VALUES];
	
	private static void populateCPV(String aaShortName, double c, double p, double v) {
		int aaInt = ResidueUtils.aaShortNameToInt(aaShortName);
		aaIntToC[aaInt] = c;
		aaIntToP[aaInt] = p;
		aaIntToV[aaInt] = v;
	}

	static {
	    populateCPV("Ser", 1.42, 9.2, 32);
	    populateCPV("Arg", 0.65, 10.5, 124);
	    populateCPV("Leu", 0, 4.9, 111);
	    populateCPV("Pro", 0.39, 8.0, 32.5);
	    populateCPV("Thr", 0.71, 8.6, 61);
	    populateCPV("Ala", 0, 8.1, 31);
	    populateCPV("Val", 0, 5.9, 84);
	    populateCPV("Gly", 0.74, 9.0, 3);
	    populateCPV("Ile", 0, 5.2, 111);
	    populateCPV("Phe", 0, 5.2, 132);
	    populateCPV("Tyr", 0.20, 6.2, 136);
	    populateCPV("Cys", 2.75, 5.5, 55);
	    populateCPV("His", 0.58, 10.4, 96);
	    populateCPV("Gln", 0.89, 10.5, 85);
	    populateCPV("Asn", 1.33, 11.6, 56);
	    populateCPV("Lys", 0.33, 11.3, 119);
	    populateCPV("Asp", 1.38, 13.0, 54);
	    populateCPV("Glu", 0.92, 12.3, 83);
	    populateCPV("Met", 0, 5.7, 105);
	    populateCPV("Trp", 0.13, 5.4, 170);
	}
	
	static {
		for(int i = 0; i < ResidueUtils.AA_NUM_VALUES; i++) {
			for(int j = 0; j < ResidueUtils.AA_NUM_VALUES; j++) {
				aaIntsToGrantham[i][j] = 50.723 * Math.sqrt((
						( alpha * Math.pow(aaIntToC[i] - aaIntToC[j], 2) ) +
						( beta * Math.pow(aaIntToP[i] - aaIntToP[j], 2) ) +
						( gamma * Math.pow(aaIntToV[i] - aaIntToV[j], 2) ) ));
			}
		}
	}
	
	public static double granthamDistance(char originalAA, char replacementAA) {
		int originalAAint = ResidueUtils.aaToInt(originalAA);
		int replacementAAint = ResidueUtils.aaToInt(replacementAA);
		return aaIntsToGrantham[originalAAint][replacementAAint];
	}
	
	
}
