package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

public class GbFeatureInterval {

	private int startNt;
	private boolean incompleteStart;
	private int endNt;
	private boolean incompleteEnd;
	
	public GbFeatureInterval(int startNt, boolean incompleteStart, int endNt, boolean incompleteEnd) {
		super();
		this.startNt = startNt;
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
	
	
}
