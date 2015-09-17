package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation.NotifiabilityLevel;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.transcription.TranscriptionFormat;

public class VariationDocument {

	private String name;
	private int refStart;
	private int refEnd;
	private Pattern regex;
	private String description;
	private NotifiabilityLevel notifiabilityLevel;
	private TranscriptionFormat transcriptionFormat;
	
	public VariationDocument(String name, int refStart, int refEnd,
			Pattern regex, String description,
			NotifiabilityLevel notifiabilityLevel,
			TranscriptionFormat transcriptionFormat) {
		super();
		this.name = name;
		this.refStart = refStart;
		this.refEnd = refEnd;
		this.regex = regex;
		this.description = description;
		this.notifiabilityLevel = notifiabilityLevel;
		this.transcriptionFormat = transcriptionFormat;
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
	public NotifiabilityLevel getNotifiabilityLevel() {
		return notifiabilityLevel;
	}
	public TranscriptionFormat getTranscriptionFormat() {
		return transcriptionFormat;
	}
	
	public void toDocument(ObjectBuilder objBuilder) {
		objBuilder.set("name", name);
		objBuilder.set("refStart", refStart);
		objBuilder.set("refEnd", refEnd);
		objBuilder.set("regex", regex.pattern());
		objBuilder.set("description", description);
		objBuilder.set("notifiability", notifiabilityLevel.name());
		objBuilder.set("transcriptionType", transcriptionFormat.name());
		
	}
	
}
