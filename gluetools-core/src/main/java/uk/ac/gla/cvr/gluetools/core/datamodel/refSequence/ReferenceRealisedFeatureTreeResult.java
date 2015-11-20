package uk.ac.gla.cvr.gluetools.core.datamodel.refSequence;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.segments.AaReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;

/**
 * This result object encapsulates all the feature locations of a reference sequence, 
 * but also includes their realised NT and AA segments.
 */

public class ReferenceRealisedFeatureTreeResult extends ReferenceFeatureTreeResult {

	private List<NtReferenceSegment> ntReferenceSegments = null;
	private List<AaReferenceSegment> aaReferenceSegments = null;

	protected ReferenceRealisedFeatureTreeResult(String referenceName) {
		super(referenceName);
	}

	protected ReferenceRealisedFeatureTreeResult(String referenceName, ReferenceRealisedFeatureTreeResult parentTreeResult, ObjectBuilder objectBuilder) {
		super(referenceName, parentTreeResult, objectBuilder);
	}

	protected ReferenceRealisedFeatureTreeResult addFeature(Feature feature) {
		return (ReferenceRealisedFeatureTreeResult) super.addFeature(feature);
	}
	

	
	public ReferenceRealisedFeatureTreeResult addFeatureLocation(CommandContext cmdContext, FeatureLocation featureLocation) {
		ReferenceRealisedFeatureTreeResult featureTreeResult = 
				(ReferenceRealisedFeatureTreeResult) super.addFeatureLocation(cmdContext, featureLocation);
		featureTreeResult.realiseSegments(cmdContext, featureLocation);
		return featureTreeResult;
	}
	
	private void realiseSegments(CommandContext cmdContext, FeatureLocation featureLocation) {
		ntReferenceSegments = featureLocation.getReferenceSequence()
				.getSequence().getSequenceObject().getNtReferenceSegments(getReferenceSegments(), cmdContext);
		ArrayBuilder ntRefSegArray = getObjectBuilder().setArray("ntReferenceSegment");
		ntReferenceSegments.forEach(ntRefSeg -> {
			ntRefSeg.toDocument(ntRefSegArray.addObject());
		});
		String orfAncestorFeatureName = getOrfAncestorFeatureName();
		if(featureLocation.getFeature().isOpenReadingFrame()) {
			aaReferenceSegments = featureLocation.translate(cmdContext);
		} else {
			if(orfAncestorFeatureName != null) {
				ReferenceRealisedFeatureTreeResult ancestorTreeResult = (ReferenceRealisedFeatureTreeResult) findAncestor(orfAncestorFeatureName);
				List<AaReferenceSegment> ancestorAaRefSegs = ancestorTreeResult.aaReferenceSegments;
				Integer orfAncestorCodon1Start = ancestorTreeResult.getCodon1Start();

				// find the AA locations of this feature, using the ORF's codon coordinates.
				List<ReferenceSegment> templateAaRefSegs = 
						TranslationUtils.translateToCodonCoordinates(orfAncestorCodon1Start, ntReferenceSegments);

				aaReferenceSegments = ReferenceSegment.intersection(templateAaRefSegs, ancestorAaRefSegs, 
						(templateSeg, ancestorSeg) -> {
							int refStart = Math.max(templateSeg.getRefStart(), ancestorSeg.getRefStart());
							int refEnd = Math.min(templateSeg.getRefEnd(), ancestorSeg.getRefEnd());
							CharSequence aminoAcids = ancestorSeg.getAminoAcidsSubsequence(refStart, refEnd);
							return new AaReferenceSegment(refStart, refEnd, aminoAcids);
						});

				Integer codon1Start = getCodon1Start();
				// if necessary translate to feature's own codon coordinates.
				if(codon1Start != null && !codon1Start.equals(orfAncestorCodon1Start)) {
					int ntOffset = orfAncestorCodon1Start-codon1Start;
					int ancestorToLocalCodonOffset = ntOffset/3;
					aaReferenceSegments.forEach(aaRefSeg -> aaRefSeg.translate(ancestorToLocalCodonOffset));
				}
			}
		}
		if(aaReferenceSegments != null) {
			ArrayBuilder aaRefSegArray = getObjectBuilder().setArray("aaReferenceSegment");
			aaReferenceSegments.forEach(aaRefSeg -> {
				aaRefSeg.toDocument(aaRefSegArray.addObject());
			});
		}
	}

	
	@Override
	public List<ReferenceRealisedFeatureTreeResult> getChildTrees() {
		List<? extends ReferenceFeatureTreeResult> childTrees = super.getChildTrees();
		List<ReferenceRealisedFeatureTreeResult> result = new ArrayList<ReferenceRealisedFeatureTreeResult>();
		for(ReferenceFeatureTreeResult childTree: childTrees) {
			result.add((ReferenceRealisedFeatureTreeResult) childTree);
		}
		return result;
	}

	protected ReferenceFeatureTreeResult createChildFeatureTreeResult(
			ReferenceFeatureTreeResult parentFeatureTreeResult,
			ObjectBuilder objectBuilder) {
		return new ReferenceRealisedFeatureTreeResult(getReferenceName(), (ReferenceRealisedFeatureTreeResult) parentFeatureTreeResult, objectBuilder);
	}
	
	public List<NtReferenceSegment> getNtReferenceSegments() {
		return ntReferenceSegments;
	}

	public List<AaReferenceSegment> getAaReferenceSegments() {
		return aaReferenceSegments;
	}


	
}