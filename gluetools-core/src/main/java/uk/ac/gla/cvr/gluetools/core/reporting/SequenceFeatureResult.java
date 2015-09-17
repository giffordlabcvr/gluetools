package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceFeatureTreeResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationDocument;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.segments.AaReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.transcription.TranscriptionFormat;
import uk.ac.gla.cvr.gluetools.core.transcription.TranscriptionUtils;


public class SequenceFeatureResult {
	private List<NtQueryAlignedSegment> ntQueryAlignedSegments;
	private List<AaReferenceSegment> aaQueryAlignedSegments;
	private List<SequenceDifference> ntSequenceDifferences = new ArrayList<SequenceDifference>();
	private List<SequenceDifference> aaSequenceDifferences = new ArrayList<SequenceDifference>();
	private ReferenceFeatureTreeResult featureTreeResult;
	
	public SequenceFeatureResult(ReferenceFeatureTreeResult featureTreeResult) {
		this.featureTreeResult = featureTreeResult;
	}

	public void init(
			AbstractSequenceObject seqObj,
			List<QueryAlignedSegment> seqToRefAlignedSegments, 
			Map<String, SequenceFeatureResult> featureToSequenceFeatureResult) {
		// intersect the seqToRefAlignedSegments with the reference segments of the feature we are looking at
		List<QueryAlignedSegment> featureQueryAlignedSegments = 
				ReferenceSegment.intersection(featureTreeResult.getReferenceSegments(), seqToRefAlignedSegments, 
						new SegMerger());
		// realize these segments (add NTs)
		ntQueryAlignedSegments = 
				seqObj.getNtQueryAlignedSegments(featureQueryAlignedSegments);
		if(ntQueryAlignedSegments.isEmpty()) {
			aaQueryAlignedSegments = new ArrayList<AaReferenceSegment>();
		} else {
			if(featureTreeResult.isOpenReadingFrame()) {
				transcribeOpenReadingFrame(seqObj);
			} else {
				String orfAncestorFeatureName = featureTreeResult.getOrfAncestorFeatureName();
				if(orfAncestorFeatureName != null) {
					SequenceFeatureResult orfAncestorSequenceFeatureResult = featureToSequenceFeatureResult.get(orfAncestorFeatureName);
					transcribeOrfDescendent(orfAncestorSequenceFeatureResult);
				}
			}
		}
		computeDifferences();
	}

	private void computeDifferences() {
		List<VariationDocument> variationDocuments = featureTreeResult.getVariationDocuments();
		for(VariationDocument variationDocument: variationDocuments) {
			List<ReferenceSegment> variationTemplateRegion = Collections.singletonList(new ReferenceSegment(variationDocument.getRefStart(), variationDocument.getRefEnd()));
			if(variationDocument.getTranscriptionFormat() == TranscriptionFormat.NUCLEOTIDE) {
				List<NtQueryAlignedSegment> queryNtVariationRegion = ReferenceSegment.intersection(ntQueryAlignedSegments, 
						variationTemplateRegion, 
						ReferenceSegment.cloneLeftSegMerger());
				if(!ReferenceSegment.sameRegion(queryNtVariationRegion, variationTemplateRegion)) {
					continue;
				}
				String queryVariationNts = String.join("", 
						queryNtVariationRegion.stream().map(region -> region.getNucleotides()).collect(Collectors.toList()));
				if(variationDocument.getRegex().matcher(queryVariationNts).find()) {
					ntSequenceDifferences.add(new SequenceDifference(variationDocument.getName(), 
							variationDocument.getRefStart(), variationDocument.getRefEnd()));
				}
			} else if(variationDocument.getTranscriptionFormat() == TranscriptionFormat.AMINO_ACID) {
				List<AaReferenceSegment> queryAaVariationRegion = ReferenceSegment.intersection(aaQueryAlignedSegments, 
						variationTemplateRegion, 
						ReferenceSegment.cloneLeftSegMerger());
				if(!ReferenceSegment.sameRegion(queryAaVariationRegion, variationTemplateRegion)) {
					continue;
				}
				String queryVariationAas = String.join("", 
						queryAaVariationRegion.stream().map(region -> region.getAminoAcids()).collect(Collectors.toList()));
				if(variationDocument.getRegex().matcher(queryVariationAas).find()) {
					aaSequenceDifferences.add(new SequenceDifference(variationDocument.getName(), 
							variationDocument.getRefStart(), variationDocument.getRefEnd()));
				}
			}
		}
	}

	public void transcribeOrfDescendent(
			SequenceFeatureResult orfAncestorSequenceFeatureResult) {
		Integer codon1Start = featureTreeResult.getCodon1Start();
		ReferenceFeatureTreeResult orfAncestorFeatureTreeResult = orfAncestorSequenceFeatureResult.featureTreeResult;
		Integer orfAncestorCodon1Start = orfAncestorFeatureTreeResult.getCodon1Start();
		List<AaReferenceSegment> ancestorAaRefSegs = orfAncestorSequenceFeatureResult.aaQueryAlignedSegments;
		Integer ancestorReferenceToQueryOffset = orfAncestorSequenceFeatureResult.ntQueryAlignedSegments.get(0).getReferenceToQueryOffset();
		
		aaQueryAlignedSegments = translateNtQuerySegsToAaQuerySegs(
				ancestorAaRefSegs, orfAncestorCodon1Start, 
				ntQueryAlignedSegments, ancestorReferenceToQueryOffset);

		// if necessary translate to feature's own codon coordinates.
		if(codon1Start != null) {
			int ntOffset = orfAncestorCodon1Start-codon1Start;
			int ancestorToLocalCodonOffset = ntOffset/3;
			aaQueryAlignedSegments.forEach(aaRefSeg -> aaRefSeg.translate(ancestorToLocalCodonOffset));
		}
	}

	public void transcribeOpenReadingFrame(AbstractSequenceObject seqObj) {
		int firstNtQuerySegRefStart = ntQueryAlignedSegments.get(0).getRefStart();
		int firstNtRefSegStart = featureTreeResult.getNtReferenceSegments().get(0).getRefStart();
		if(firstNtQuerySegRefStart != firstNtRefSegStart) {
			// query segments fail to cover the start codon, so we can't transcribe.
			aaQueryAlignedSegments = new ArrayList<AaReferenceSegment>();
		} else {
			int seqFeatureQueryNtStart = ntQueryAlignedSegments.get(0).getQueryStart();
			int seqFeatureQueryNtEnd = ntQueryAlignedSegments.get(ntQueryAlignedSegments.size()-1).getQueryEnd();

			// attempt to transcribe everything between the start and end points.
			// the point of this is to pick up any possible stop codons or gaps in the gaps between aligned
			// segments
			String seqFeatureAAs = TranscriptionUtils.transcribe(
					seqObj.getNucleotides(seqFeatureQueryNtStart, seqFeatureQueryNtEnd));
			if(seqFeatureAAs.length() == 0) {
				aaQueryAlignedSegments = new ArrayList<AaReferenceSegment>();
			} else {
				aaQueryAlignedSegments = new ArrayList<AaReferenceSegment>();
				int firstSegRefToQueryOffset = ntQueryAlignedSegments.get(0).getReferenceToQueryOffset();
				for(NtQueryAlignedSegment ntQuerySeg : ntQueryAlignedSegments) {
					int segReferenceToQueryOffset = ntQuerySeg.getReferenceToQueryOffset();
					if( (segReferenceToQueryOffset - firstSegRefToQueryOffset) % 3 != 0 ) {
						continue; // skip any query segments which change the reading frame.
					}
					// conservatively select the transcribed chunk which is fully covered by this segment.
					int querySegNtStart = ntQuerySeg.getQueryStart();
					int firstQuerySegCodon = TranscriptionUtils.getCodon(seqFeatureQueryNtStart, querySegNtStart);
					if(!TranscriptionUtils.isAtStartOfCodon(seqFeatureQueryNtStart, querySegNtStart)) {
						firstQuerySegCodon++;
					}
					int querySegNtEnd = ntQuerySeg.getQueryEnd();
					int lastQuerySegCodon = TranscriptionUtils.getCodon(seqFeatureQueryNtStart, querySegNtEnd);
					if(!TranscriptionUtils.isAtEndOfCodon(seqFeatureQueryNtStart, querySegNtEnd)) {
						lastQuerySegCodon--;
					}
					if(lastQuerySegCodon < firstQuerySegCodon) {
						continue;
					}
					if(firstQuerySegCodon >= seqFeatureAAs.length()) {
						continue;
					}
					lastQuerySegCodon = Math.min(lastQuerySegCodon, seqFeatureAAs.length());
					CharSequence querySegAas = seqFeatureAAs.subSequence(firstQuerySegCodon-1, lastQuerySegCodon);
					// translate the location back to reference codon numbers
					int refNTStart = TranscriptionUtils.getNt(seqFeatureQueryNtStart, firstQuerySegCodon) + 
							ntQuerySeg.getQueryToReferenceOffset();
					int refCodonStart = TranscriptionUtils.getCodon(featureTreeResult.getCodon1Start(), refNTStart);
					int refCodonEnd = refCodonStart+(querySegAas.length()-1);
					aaQueryAlignedSegments.add(new AaReferenceSegment(refCodonStart, refCodonEnd, querySegAas));								
				}
			}
		}
	}

	// Given a list of aa segments covering the transcribed area, and a codon 1 start location
	// narrow this down to the set of aa segments for those reference regions covered by ntQueryAlignedSegments.
	// referenceToQueryOffset implicitly specifies the reading frame -- ntQueryAlignedSegments whose offset implies 
	// a different reading frame must be filtered out.
	private List<AaReferenceSegment> translateNtQuerySegsToAaQuerySegs(
			List<AaReferenceSegment> aaReferenceSegments,
			Integer codon1Start,
			List<NtQueryAlignedSegment> ntQueryAlignedSegments, 
			Integer referenceToQueryOffset) {
		
		// Let x be the reference -> query offset specifying the reading frame
		// If any query NT segment proposes an offset which is not equal to x 
		// plus/minus some multiple of 3, possibly zero, that segment must be discarded.
		List<QueryAlignedSegment> filteredNtSegments = ntQueryAlignedSegments
				.stream()
				.filter(ntSeg -> ( (ntSeg.getReferenceToQueryOffset() - referenceToQueryOffset) % 3 == 0 ))
				.collect(Collectors.toList());

		// find the AA codon coordinates of the filtered NT segments, 
		// using the ORF's codon coordinates.
		List<ReferenceSegment> templateAaRefSegs = 
				TranscriptionUtils.translateToCodonCoordinates(codon1Start, filteredNtSegments);
		// intersect this template with the already transcribed segment to get the final Query AA segments.
		return ReferenceSegment.intersection(templateAaRefSegs, aaReferenceSegments, 
				(templateSeg, aaRefSeg) -> {
					int refStart = Math.max(templateSeg.getRefStart(), aaRefSeg.getRefStart());
					int refEnd = Math.min(templateSeg.getRefEnd(), aaRefSeg.getRefEnd());
					CharSequence aminoAcids = aaRefSeg.getAminoAcidsSubsequence(refStart, refEnd);
					return new AaReferenceSegment(refStart, refEnd, aminoAcids);
				}
		);
	}

	private class SegMerger implements BiFunction<ReferenceSegment, QueryAlignedSegment, QueryAlignedSegment> {
		@Override
		public QueryAlignedSegment apply(ReferenceSegment refSeg, QueryAlignedSegment querySeg) {
			int refStart = Math.max(refSeg.getRefStart(), querySeg.getRefStart());
			int refEnd = Math.min(refSeg.getRefEnd(), querySeg.getRefEnd());
			int refToQueryOffset = querySeg.getQueryStart() - querySeg.getRefStart();
			
			return new QueryAlignedSegment(refStart, refEnd, refStart+refToQueryOffset, refEnd+refToQueryOffset);
		}
	}
	
	public void toDocument(ObjectBuilder seqFeatureResultObj) {
		seqFeatureResultObj.set("featureName", featureTreeResult.getFeatureName());
		ArrayBuilder ntQuerySegArray = seqFeatureResultObj.setArray("ntQueryAlignedSegment");
		for(NtQueryAlignedSegment ntQueryAlignedSegment: ntQueryAlignedSegments) {
			ntQueryAlignedSegment.toDocument(ntQuerySegArray.addObject());
		}
		if(aaQueryAlignedSegments != null) {
			ArrayBuilder aaRefSegArray = seqFeatureResultObj.setArray("aaQueryAlignedSegment");
			for(AaReferenceSegment aaReferenceSegment: aaQueryAlignedSegments) {
				aaReferenceSegment.toDocument(aaRefSegArray.addObject());
			}
		}
		ArrayBuilder aaSeqDiffArray = seqFeatureResultObj.setArray("aaSequenceDifference");
		for(SequenceDifference aaSequenceDifference: aaSequenceDifferences) {
			aaSequenceDifference.toDocument(aaSeqDiffArray.addObject());
		}
		ArrayBuilder ntSeqDiffArray = seqFeatureResultObj.setArray("ntSequenceDifference");
		for(SequenceDifference ntSequenceDifference: ntSequenceDifferences) {
			ntSequenceDifference.toDocument(ntSeqDiffArray.addObject());
		}
	}
}