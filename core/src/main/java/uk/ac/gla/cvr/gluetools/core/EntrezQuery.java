package uk.ac.gla.cvr.gluetools.core;

public class EntrezQuery {

	private String ncbiDB = "nuccore";
	
	private String searchTerm = "Hepatitis%20C[Organism]+AND+7000:10000[SLEN]";

	public String getNcbiDB() {
		return ncbiDB;
	}

	public String getSearchTerm() {
		return searchTerm;
	}
	
	
	
	
}
