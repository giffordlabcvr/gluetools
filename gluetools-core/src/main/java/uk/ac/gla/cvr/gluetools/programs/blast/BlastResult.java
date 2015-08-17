package uk.ac.gla.cvr.gluetools.programs.blast;

import java.util.ArrayList;
import java.util.List;

public class BlastResult {

	private String queryFastaId;
	private List<BlastHit> hits = new ArrayList<BlastHit>();
	
	public void addHit(BlastHit hit) {
		hits.add(hit);
	}
	
	public List<BlastHit> getHits() {
		return hits;
	}

	public String getQueryFastaId() {
		return queryFastaId;
	}

	public void setQueryFastaId(String queryFastaId) {
		this.queryFastaId = queryFastaId;
	}
	
}
