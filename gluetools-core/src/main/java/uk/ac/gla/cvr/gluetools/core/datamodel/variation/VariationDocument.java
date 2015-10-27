package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.reporting.contentNotes.VariationNote;
import uk.ac.gla.cvr.gluetools.core.segments.IAaReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.INtReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
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
	
	public VariationDocument(String name, int refStart, int refEnd,
			Pattern regex, String description,
			TranslationFormat transcriptionFormat) {
		super();
		this.name = name;
		this.refStart = refStart;
		this.refEnd = refEnd;
		this.regex = regex;
		this.description = description;
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
	public TranslationFormat getTranscriptionFormat() {
		return transcriptionFormat;
	}
	
	public void toDocument(ObjectBuilder objBuilder) {
		objBuilder.set("name", name);
		objBuilder.set("refStart", refStart);
		objBuilder.set("refEnd", refEnd);
		objBuilder.set("regex", regex.pattern());
		objBuilder.set("description", description);
		objBuilder.set("transcriptionType", transcriptionFormat.name());
	}
	
	public <S extends INtReferenceSegment> VariationNote generateNtVariationNote(List<S> ntQueryAlignedSegments) {
		List<ReferenceSegment> variationTemplateRegion = 
				Collections.singletonList(new ReferenceSegment(getRefStart(), getRefEnd()));

		List<S> queryNtVariationRegion = ReferenceSegment.intersection(ntQueryAlignedSegments, 
				variationTemplateRegion, 
				ReferenceSegment.cloneLeftSegMerger());
		if(!ReferenceSegment.sameRegion(queryNtVariationRegion, variationTemplateRegion)) {
			return null;
		}
		String queryVariationNts = String.join("", 
				queryNtVariationRegion.stream().map(region -> region.getNucleotides()).collect(Collectors.toList()));
		if(getRegex().matcher(queryVariationNts).find()) {
			return new VariationNote(getName(), getRefStart(), getRefEnd());
		}
		return null;
	}

	
	public <S extends IAaReferenceSegment> VariationNote generateAaVariationNote(List<S> aaQueryAlignedSegments) {
		List<ReferenceSegment> variationTemplateRegion = 
				Collections.singletonList(new ReferenceSegment(getRefStart(), getRefEnd()));

		List<S> queryAaVariationRegion = ReferenceSegment.intersection(aaQueryAlignedSegments, 
				variationTemplateRegion, 
				ReferenceSegment.cloneLeftSegMerger());
		if(!ReferenceSegment.sameRegion(queryAaVariationRegion, variationTemplateRegion)) {
			return null;
		}
		String queryVariationAas = String.join("", 
				queryAaVariationRegion.stream().map(region -> region.getAminoAcids()).collect(Collectors.toList()));
		if(getRegex().matcher(queryVariationAas).find()) {
			return new VariationNote(getName(), getRefStart(), getRefEnd());
		}
		return null;
	}

	
}
