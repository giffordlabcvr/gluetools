package uk.ac.gla.cvr.gluetools.core.reporting.contentNotes;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

/**
 * A difference summary note is a summary of a difference between the query and reference, summarised using a string notation. 
 * It may or may not relate to a named variation.
 */
public class DifferenceSummaryNote {

	private String summaryString;
	private String variationName;
	
	
	public DifferenceSummaryNote(String summaryString, String variationName) {
		this.summaryString = summaryString;
		this.variationName = variationName;
	}

	
	public void toDocument(ObjectBuilder objBuilder) {
		objBuilder.setString("summaryString", summaryString);
		if(variationName != null) {
			objBuilder.setString("variationName", variationName);
		}
	}


	public String getSummaryString() {
		return summaryString;
	}
	
}
