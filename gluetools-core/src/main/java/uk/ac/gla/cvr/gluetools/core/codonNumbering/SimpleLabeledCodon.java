package uk.ac.gla.cvr.gluetools.core.codonNumbering;

import java.util.Arrays;

public class SimpleLabeledCodon extends LabeledCodon {

	private int ntMiddle;
	
	public SimpleLabeledCodon(String featureName, String codonLabel, int ntStart, int ntMiddle, int ntEnd,
			int transcriptionIndex) {
		super(featureName, codonLabel, Arrays.asList(ntStart, ntMiddle, ntEnd), transcriptionIndex);
		this.ntMiddle = ntMiddle;
	}

	public int getNtMiddle() {
		return ntMiddle;
	}
	
	
	
}
