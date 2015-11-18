package uk.ac.gla.cvr.gluetools.core.reporting.contentNotes;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

/**
 * A difference summary note is a summary of a difference between the query and reference, summarised using a string notation. 
 * It may also relate to a set of named variations.
 */
public class DifferenceSummaryNote {

	private String summaryString;
	private List<String> variationNames;
	
	
	public DifferenceSummaryNote(String summaryString, List<String> variationNames) {
		this.summaryString = summaryString;
		this.variationNames = variationNames;
	}

	
	public void toDocument(ObjectBuilder objBuilder) {
		objBuilder.setString("summaryString", summaryString);
		if(variationNames != null) {
			ArrayBuilder variationNamesArrayBuilder = objBuilder.setArray("variationName");
			for(String variationName: variationNames) {
				variationNamesArrayBuilder.add(variationName);
			}
		}
	}


	public String getSummaryString() {
		return summaryString;
	}
	
}
