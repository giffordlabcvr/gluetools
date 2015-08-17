package uk.ac.gla.cvr.gluetools.programs.blast;

import java.util.ArrayList;
import java.util.List;

public class BlastHit {

	private String referenceName;
	private List<BlastHsp> hsps = new ArrayList<BlastHsp>();
	private BlastResult blastResult;
	
	public BlastHit(BlastResult blastResult) {
		super();
		this.blastResult = blastResult;
	}
	public String getReferenceName() {
		return referenceName;
	}
	public void setReferenceName(String referenceName) {
		this.referenceName = referenceName;
	}

	public void addHsp(BlastHsp hsp) {
		hsps.add(hsp);
	}
	
	public List<BlastHsp> getHsps() {
		return hsps;
	}

	public BlastResult getBlastResult() {
		return blastResult;
	}
	
}
