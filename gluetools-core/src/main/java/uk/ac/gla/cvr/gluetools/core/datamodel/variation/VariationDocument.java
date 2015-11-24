package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationFormat;

/**
 * Representation of a variation definition which is easy to serialize, and which can be used for computation.
 */
public class VariationDocument {

	private String name;
	private int refStart;
	private int refEnd;
	private Pattern regex;
	private String description;
	private TranslationFormat transcriptionFormat;
	private LinkedHashSet<String> variationCategories;
	
	public VariationDocument(String name, int refStart, int refEnd,
			Pattern regex, String description,
			TranslationFormat transcriptionFormat, 
			LinkedHashSet<String> variationCategories) {
		super();
		this.name = name;
		this.refStart = refStart;
		this.refEnd = refEnd;
		this.regex = regex;
		this.description = description;
		this.transcriptionFormat = transcriptionFormat;
		this.variationCategories = variationCategories;
	}

	public String getName() {
		return name;
	}
	public int getRefStart() {
		return refStart;
	}
	public int getRefEnd() {
		return refEnd;
	}
	public Pattern getRegex() {
		return regex;
	}
	public String getDescription() {
		return description;
	}
	public TranslationFormat getTranscriptionFormat() {
		return transcriptionFormat;
	}
	
	public void toDocument(ObjectBuilder objBuilder) {
		objBuilder.set("name", name);
		objBuilder.set("refStart", refStart);
		objBuilder.set("refEnd", refEnd);
		if(regex != null) {
			objBuilder.set("regex", regex.pattern());
		}
		objBuilder.set("description", description);
		objBuilder.set("transcriptionType", transcriptionFormat.name());
		ArrayBuilder vcatArrayBuilder = objBuilder.setArray("variationCategory");
		for(String variationCategory: variationCategories) {
			vcatArrayBuilder.add(variationCategory);
		}
		
	}
	
	
	public LinkedHashSet<String> getVariationCategories() {
		return variationCategories;
	}

	
}
