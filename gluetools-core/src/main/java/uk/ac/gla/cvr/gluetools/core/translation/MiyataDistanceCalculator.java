package uk.ac.gla.cvr.gluetools.core.translation;

public class MiyataDistanceCalculator {

	private static double aaIntToP[] = new double[ResidueUtils.AA_NUM_VALUES];
	private static double aaIntToV[] = new double[ResidueUtils.AA_NUM_VALUES];

	
	// map two AA ints to their Grantham Distance
	private static double aaIntsToMiyata[][] = new double[ResidueUtils.AA_NUM_VALUES][ResidueUtils.AA_NUM_VALUES];
	
	private static void populatePV(String aaShortName, double p, double v) {
		int aaInt = ResidueUtils.aaShortNameToInt(aaShortName);
		aaIntToP[aaInt] = p;
		aaIntToV[aaInt] = v;
	}

	static {
	    populatePV("Ser", 9.2, 32);
	    populatePV("Arg", 10.5, 124);
	    populatePV("Leu", 4.9, 111);
	    populatePV("Pro", 8.0, 32.5);
	    populatePV("Thr", 8.6, 61);
	    populatePV("Ala", 8.1, 31);
	    populatePV("Val", 5.9, 84);
	    populatePV("Gly", 9.0, 3);
	    populatePV("Ile", 5.2, 111);
	    populatePV("Phe", 5.2, 132);
	    populatePV("Tyr", 6.2, 136);
	    populatePV("Cys", 5.5, 55);
	    populatePV("His", 10.4, 96);
	    populatePV("Gln", 10.5, 85);
	    populatePV("Asn", 11.6, 56);
	    populatePV("Lys", 11.3, 119);
	    populatePV("Asp", 13.0, 54);
	    populatePV("Glu", 12.3, 83);
	    populatePV("Met", 5.7, 105);
	    populatePV("Trp", 5.4, 170);
	}
	
	static {
		double totalDeltaPij = 0.0;
		double totalDeltaVij = 0.0;
		int numCases = 0;
		for(int i = 0; i < ResidueUtils.AA_NUM_VALUES; i++) {
			if(i != ResidueUtils.AA_STOP) {
				for(int j = 0; j < ResidueUtils.AA_NUM_VALUES; j++) {
					if(j != ResidueUtils.AA_STOP && j != i) {
						double deltaPij = Math.abs(aaIntToP[i] - aaIntToP[j]);
						totalDeltaPij += deltaPij;
						double deltaVij = Math.abs(aaIntToV[i] - aaIntToV[j]);
						totalDeltaVij += deltaVij;
						numCases++;
					}
				}
			}
		}
		double meanDeltaPij = totalDeltaPij / numCases;
		double meanDeltaVij = totalDeltaVij / numCases;
		double totalSquaredDiffsDeltaPij = 0.0;
		double totalSquaredDiffsDeltaVij = 0.0;
		for(int i = 0; i < ResidueUtils.AA_NUM_VALUES; i++) {
			if(i != ResidueUtils.AA_STOP) {
				for(int j = 0; j < ResidueUtils.AA_NUM_VALUES; j++) {
					if(j != ResidueUtils.AA_STOP && j != i) {
						double deltaPij = Math.abs(aaIntToP[i] - aaIntToP[j]);
						double squaredDiffDeltaPij = Math.pow(deltaPij - meanDeltaPij, 2);
						totalSquaredDiffsDeltaPij += squaredDiffDeltaPij;
						double deltaVij = Math.abs(aaIntToV[i] - aaIntToV[j]);
						double squaredDiffDeltaVij = Math.pow(deltaVij - meanDeltaVij, 2);
						totalSquaredDiffsDeltaVij += squaredDiffDeltaVij;
					}
				}
			}
		}
		double varianceDeltaPij = totalSquaredDiffsDeltaPij / numCases;
		double varianceDeltaVij = totalSquaredDiffsDeltaVij / numCases;
		double stdDevDeltaPij = Math.sqrt(varianceDeltaPij);
		double stdDevDeltaVij = Math.sqrt(varianceDeltaVij);

		for(int i = 0; i < ResidueUtils.AA_NUM_VALUES; i++) {
			if(i != ResidueUtils.AA_STOP) {
				for(int j = 0; j < ResidueUtils.AA_NUM_VALUES; j++) {
					if(j != ResidueUtils.AA_STOP && j != i) {
						double deltaPij = Math.abs(aaIntToP[i] - aaIntToP[j]);
						double deltaVij = Math.abs(aaIntToV[i] - aaIntToV[j]);
						aaIntsToMiyata[i][j] = Math.sqrt(
								Math.pow(deltaPij / stdDevDeltaPij, 2) +
								Math.pow(deltaVij / stdDevDeltaVij, 2) 
						);
					}
				}
			}
		}
	}
	
	public static double miyataDistance(char originalAA, char replacementAA) {
		int originalAAint = ResidueUtils.aaToInt(originalAA);
		int replacementAAint = ResidueUtils.aaToInt(replacementAA);
		return aaIntsToMiyata[originalAAint][replacementAAint];
	}
	
	
}
