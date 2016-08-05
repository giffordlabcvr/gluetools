package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.math.BigDecimal;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.PlacementResult;


public class GenotypeResult {

	public enum SummaryCode {
		// sequence grouped under the type alignment and distance to closest member was under threshold
		POSITIVE_GROUPED, 
		// distance to closest member was under threshold, sequence did not group under type alignment
		// (type alignment in this case will be group containing closest member)
		POSITIVE_UNGROUPED,
		// sequence grouped under the type alignment but distance to closest member was not under threshold
		// could indicate new clade under type alignment
		OUTGROUP,
		// no known grouping.
		NEGATIVE
	}
	
	private String sequenceName;
	private String typeAlignmentName;
	private SummaryCode summaryCode;
	private PlacementResult placementResult;
	
	public String getSequenceName() {
		return sequenceName;
	}
	public void setSequenceName(String sequenceName) {
		this.sequenceName = sequenceName;
	}
	public String getTypeAlignmentName() {
		return typeAlignmentName;
	}
	public void setTypeAlignmentName(String typeAlignmentName) {
		this.typeAlignmentName = typeAlignmentName;
	}
	
	public SummaryCode getSummaryCode() {
		return summaryCode;
	}
	public void setSummaryCode(SummaryCode summaryCode) {
		this.summaryCode = summaryCode;
	}
	public PlacementResult getPlacementResult() {
		return placementResult;
	}
	public void setPlacementResult(PlacementResult placementResult) {
		this.placementResult = placementResult;
	}
	
	
}
