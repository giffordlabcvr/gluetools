package uk.ac.gla.cvr.gluetools.core.translation;

/*
 * Qualitative system for classifying amino acid replacements based on Hanada et al., 2006 
 * https://doi.org/10.1016/j.gene.2006.06.029
 */
public class Hanada2006Classification {

	private static class AminoAcidGroup {
		private String description;
		private int[] aaIntMembers;
		public AminoAcidGroup(String description, int... aaIntMembers) {
			super();
			this.description = description;
			this.aaIntMembers = aaIntMembers;
		}
	}
	
	// maps each AA (first dimension) and each classification (I, II, III, second dimension), 
	// to the AA group containing that residue for that classificaiton
	private static AminoAcidGroup[][] aaIntClassifierToGroup;
	
	public enum PropertyClassification {
		classification_I("Classification I by polarity and volume", "Polarity & Volume",
				 new AminoAcidGroup("Special", ResidueUtils.AA_C),
				 new AminoAcidGroup("Neutral and small", ResidueUtils.AA_A, ResidueUtils.AA_G, ResidueUtils.AA_P, ResidueUtils.AA_S, ResidueUtils.AA_T),
				 new AminoAcidGroup("Polar and relatively small", ResidueUtils.AA_N, ResidueUtils.AA_D, ResidueUtils.AA_Q, ResidueUtils.AA_E),
				 new AminoAcidGroup("Polar and relatively large", ResidueUtils.AA_R, ResidueUtils.AA_H, ResidueUtils.AA_K),
				 new AminoAcidGroup("Nonpolar and relatively small", ResidueUtils.AA_I, ResidueUtils.AA_L, ResidueUtils.AA_M, ResidueUtils.AA_V),
				 new AminoAcidGroup("Nonpolar and relatively large", ResidueUtils.AA_F, ResidueUtils.AA_W, ResidueUtils.AA_Y)
		),
		classification_II("Classification II by charge and aromaticity", "Charge & Aromaticity",
				  new AminoAcidGroup("Acidic", ResidueUtils.AA_D, ResidueUtils.AA_E),
				  new AminoAcidGroup("Neutral and no aromaticity", ResidueUtils.AA_Q, ResidueUtils.AA_A, ResidueUtils.AA_V, ResidueUtils.AA_L, ResidueUtils.AA_I, 
						  ResidueUtils.AA_C, ResidueUtils.AA_S, ResidueUtils.AA_T, ResidueUtils.AA_N, ResidueUtils.AA_G, ResidueUtils.AA_P, ResidueUtils.AA_M),
				  new AminoAcidGroup("Neutral and aromaticity", ResidueUtils.AA_F, ResidueUtils.AA_Y, ResidueUtils.AA_W),
				  new AminoAcidGroup("Basic", ResidueUtils.AA_K, ResidueUtils.AA_R, ResidueUtils.AA_H)
		),
		classification_III("Classification III by charge and polarity", "Charge & Polarity",
				   new AminoAcidGroup("Neutral and polarity", ResidueUtils.AA_S, ResidueUtils.AA_T, ResidueUtils.AA_Y, ResidueUtils.AA_C, ResidueUtils.AA_N, ResidueUtils.AA_Q),
				   new AminoAcidGroup("Acidic and polarity", ResidueUtils.AA_D, ResidueUtils.AA_E),
				   new AminoAcidGroup("Basic and polarity", ResidueUtils.AA_K, ResidueUtils.AA_R, ResidueUtils.AA_H),
				   new AminoAcidGroup("No polarity", ResidueUtils.AA_G, ResidueUtils.AA_A, ResidueUtils.AA_V, ResidueUtils.AA_L, ResidueUtils.AA_I, 
						   ResidueUtils.AA_F, ResidueUtils.AA_P, ResidueUtils.AA_M, ResidueUtils.AA_W)
		);
		
		private String longName;
		private String shortName;
		private AminoAcidGroup[] groups;
		
		private PropertyClassification(String longName, String shortName, AminoAcidGroup... groups) {
			this.longName = longName;
			this.shortName = shortName;
			this.groups = groups;
		}

		public String getLongName() {
			return longName;
		}

		public String getShortName() {
			return shortName;
		}
		
		
		
	}
	
	static {
		aaIntClassifierToGroup = new AminoAcidGroup[ResidueUtils.AA_NUM_VALUES][PropertyClassification.values().length];
		
		for(PropertyClassification propClass: PropertyClassification.values()) {
			for(AminoAcidGroup aaGroup: propClass.groups) {
				for(int aaInt: aaGroup.aaIntMembers) {
					aaIntClassifierToGroup[aaInt][propClass.ordinal()] = aaGroup;
				}
			}
		}
	}
	
	public static class ReplacementClassification {
		
		private String propLongName;
		private String propShortName;
		private String originalGroup;
		private String replacementGroup;
		private boolean radical;
		
		public ReplacementClassification(String propLongName, String propShortName, String originalGroup,
				String replacementGroup, boolean radical) {
			super();
			this.propLongName = propLongName;
			this.propShortName = propShortName;
			this.originalGroup = originalGroup;
			this.replacementGroup = replacementGroup;
			this.radical = radical;
		}

		public String getPropLongName() {
			return propLongName;
		}

		public String getPropShortName() {
			return propShortName;
		}

		public String getOriginalGroup() {
			return originalGroup;
		}

		public String getReplacementGroup() {
			return replacementGroup;
		}

		public boolean isRadical() {
			return radical;
		}
	}
	
	public static ReplacementClassification[] classifyReplacement(char originalAA, char replacementAA) {
		ReplacementClassification[] replClassification = new ReplacementClassification[PropertyClassification.values().length];
		int originalAAint = ResidueUtils.aaToInt(originalAA);
		int replacementAAint = ResidueUtils.aaToInt(replacementAA);
		for(PropertyClassification propClass : PropertyClassification.values()) {
			int propClassOrdinal = propClass.ordinal();
			AminoAcidGroup originalGroup = aaIntClassifierToGroup[originalAAint][propClassOrdinal];
			AminoAcidGroup replacementGroup = aaIntClassifierToGroup[replacementAAint][propClassOrdinal];
			replClassification[propClassOrdinal] = new ReplacementClassification(propClass.longName, propClass.shortName, 
					originalGroup.description, replacementGroup.description, originalGroup != replacementGroup);
		}
		return replClassification;
	}
}
