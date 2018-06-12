package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

public class GbFeatureInterval {

	private int startNt;
	private int refStartNt;
	private boolean incompleteStart;
	private int endNt;
	private boolean incompleteEnd;
	
	public GbFeatureInterval(int startNt, int refStartNt, boolean incompleteStart, int endNt, boolean incompleteEnd) {
		super();
		this.startNt = startNt;
		this.refStartNt = refStartNt;
		this.incompleteStart = incompleteStart;
		this.endNt = endNt;
		this.incompleteEnd = incompleteEnd;
	}

	public String getStartString() {
		return (incompleteStart ? "<" : "") + Integer.toString(startNt);
	}
	
	public String getEndString() {
		return (incompleteEnd ? ">" : "") + Integer.toString(endNt);
	}

	public boolean isIncompleteStart() {
		return incompleteStart;
	}

	public int getRefStartNt() {
		return refStartNt;
	}
	
	
}
