package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.List;



public class GenotypeResult {

	private String queryName;
	private List<GenotypeCategoryResult> categoryResults;
	
	public String getQueryName() {
		return queryName;
	}
	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}
	
	
}
