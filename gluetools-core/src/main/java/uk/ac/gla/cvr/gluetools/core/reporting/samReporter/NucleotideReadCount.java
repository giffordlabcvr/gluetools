package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

public class NucleotideReadCount {
	private int samRefNt;
	private int acRefNt;
	
	int readsWithA;
	int readsWithC;
	int readsWithT;
	int readsWithG;
	int totalContributingReads;
	
	public NucleotideReadCount(int samRefNt, int acRefNt) {
		super();
		this.samRefNt = samRefNt;
		this.acRefNt = acRefNt;
	}

	public int getTotalContributingReads() {
		return totalContributingReads;
	}
	
	public int getSamRefNt() {
		return samRefNt;
	}

	public int getAcRefNt() {
		return acRefNt;
	}

	public int getReadsWithA() {
		return readsWithA;
	}

	public int getReadsWithC() {
		return readsWithC;
	}

	public int getReadsWithT() {
		return readsWithT;
	}

	public int getReadsWithG() {
		return readsWithG;
	}
	
}