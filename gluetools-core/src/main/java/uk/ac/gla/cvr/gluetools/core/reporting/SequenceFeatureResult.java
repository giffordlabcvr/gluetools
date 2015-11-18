package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceFeatureTreeResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceRealisedFeatureTreeResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AaMinorityVariant;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.NtMinorityVariant;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sam2ConsensusMinorityVariantFilter;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sam2ConsensusSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationDocument;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.reporting.contentNotes.ReferenceDifferenceNote;
import uk.ac.gla.cvr.gluetools.core.reporting.contentNotes.VariationNote;
import uk.ac.gla.cvr.gluetools.core.segments.AaReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationFormat;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;


public class SequenceFeatureResult {
	private List<NtQueryAlignedSegment> ntQueryAlignedSegments = new ArrayList<NtQueryAlignedSegment>();
	private List<AaReferenceSegment> aaQueryAlignedSegments = new ArrayList<AaReferenceSegment>();
	private List<VariationNote> ntVariationNotes = new ArrayList<VariationNote>();
	private List<VariationNote> aaVariationNotes = new ArrayList<VariationNote>();
	private List<ReferenceDifferenceNote> ntReferenceDifferenceNotes = new ArrayList<ReferenceDifferenceNote>();
	private List<ReferenceDifferenceNote> aaReferenceDifferenceNotes = new ArrayList<ReferenceDifferenceNote>();
	
	
	private ReferenceRealisedFeatureTreeResult featureTreeResult;
	
	private List<NtMinorityVariant> ntMinorityVariants = new ArrayList<NtMinorityVariant>();
	private List<AaMinorityVariant> aaMinorityVariants = new ArrayList<AaMinorityVariant>();

	
	public SequenceFeatureResult(ReferenceRealisedFeatureTreeResult featureTreeResult) {
		this.featureTreeResult = featureTreeResult;
	}

	public void init(
			CommandContext cmdContext,
			AbstractSequenceObject querySeqObj,
			List<QueryAlignedSegment> seqToRefAlignedSegments, 
			Map<String, SequenceFeatureResult> featureToSequenceFeatureResult, 
			Sam2ConsensusMinorityVariantFilter s2cMinorityVariantFilter) {
		// intersect the seqToRefAlignedSegments with the reference segments of the feature we are looking at
		List<QueryAlignedSegment> featureQueryAlignedSegments = 
				ReferenceSegment.intersection(featureTreeResult.getReferenceSegments(), seqToRefAlignedSegments, 
						new SegMerger());
		// realize these segments (add NTs)
		ntQueryAlignedSegments.addAll(querySeqObj.getNtQueryAlignedSegments(featureQueryAlignedSegments, cmdContext));
		
		generateNtMinorityVariants(querySeqObj, s2cMinorityVariantFilter);
		
		if(ntQueryAlignedSegments.isEmpty()) {
			return;
		} else {
			boolean isORF = featureTreeResult.isOpenReadingFrame();
			boolean translateOrfDescendentsDirectly = 
					cmdContext.getProjectSettingValue(ProjectSettingOption.TRANSLATE_ORF_DESCENDENTS_DIRECTLY).equals("true");
			String orfAncestorFeatureName = featureTreeResult.getOrfAncestorFeatureName();
			if(isORF || (orfAncestorFeatureName != null && translateOrfDescendentsDirectly) ) {
				translateDirectly(isORF, querySeqObj, cmdContext);
			} else {
				if(orfAncestorFeatureName != null) {
					SequenceFeatureResult orfAncestorSequenceFeatureResult = featureToSequenceFeatureResult.get(orfAncestorFeatureName);
					deriveTranslationFromOrfAncestor(orfAncestorSequenceFeatureResult);
				}
			}
		}
		generateContentNotes();
	}


	private void generateNtMinorityVariants(AbstractSequenceObject querySeqObj,
			Sam2ConsensusMinorityVariantFilter s2cMinorityVariantFilter) {
		if(querySeqObj instanceof Sam2ConsensusSequenceObject) {
			Sam2ConsensusSequenceObject s2cSeqObj = (Sam2ConsensusSequenceObject) querySeqObj;
			for(NtQueryAlignedSegment ntQuerySeg: ntQueryAlignedSegments) {
				ntMinorityVariants.addAll(s2cSeqObj.getMinorityVariants(ntQuerySeg.getQueryStart(), 
						ntQuerySeg.getQueryEnd(), s2cMinorityVariantFilter));
			}
		}
	}

	private void generateContentNotes() {
		List<VariationDocument> variationDocuments = featureTreeResult.getVariationDocuments();
		for(VariationDocument variationDocument: variationDocuments) {
			if(variationDocument.getTranscriptionFormat() == TranslationFormat.NUCLEOTIDE) {
				VariationNote variationNote = variationDocument.generateNtVariationNote(ntQueryAlignedSegments);
				if(variationNote != null) {
					ntVariationNotes.add(variationNote);
				}
			}
		}
		generateNtDifferenceNotes(featureTreeResult.getNtReferenceSegments());
		
		
		if(aaQueryAlignedSegments != null) {
			for(VariationDocument variationDocument: variationDocuments) {
				if(variationDocument.getTranscriptionFormat() == TranslationFormat.AMINO_ACID) {
					VariationNote variationNote = variationDocument.generateAaVariationNote(aaQueryAlignedSegments);
					if(variationNote != null) {
						aaVariationNotes.add(variationNote);
					}
				}
			}
			final Map<Integer, List<VariationNote>> refStartToVariationNotes = new LinkedHashMap<Integer, List<VariationNote>>();
			for(VariationNote aaVariationNote: aaVariationNotes) {
				refStartToVariationNotes.compute(aaVariationNote.getRefStart(), 
					(k, list) -> 
					{ 
						if(list == null) { 
							list = new ArrayList<VariationNote>(); 
						} 
						list.add(aaVariationNote); 
						return list;
					});
			}
			
			List<AaReferenceSegment> aaReferenceSegments = featureTreeResult.getAaReferenceSegments();
			if(aaReferenceSegments != null) {
				aaReferenceDifferenceNotes = generateAaDifferenceNotes(aaReferenceSegments, aaQueryAlignedSegments, refStartToVariationNotes);
			}
		}
	}


	private void generateNtDifferenceNotes(
			List<NtReferenceSegment> ntReferenceSegments) {
		ntReferenceDifferenceNotes.addAll(ReferenceSegment.intersection(ntReferenceSegments, ntQueryAlignedSegments, 
				new BiFunction<NtReferenceSegment, NtQueryAlignedSegment, ReferenceDifferenceNote>() {
					@Override
					public ReferenceDifferenceNote apply(NtReferenceSegment ntRefSeg, NtQueryAlignedSegment ntQuerySeg) {
						int refStart = Math.max(ntRefSeg.getRefStart(), ntQuerySeg.getRefStart());
						int refEnd = Math.min(ntRefSeg.getRefEnd(), ntQuerySeg.getRefEnd());
						CharSequence refNts = ntRefSeg.getNucleotidesSubsequence(refStart, refEnd);
						CharSequence queryNts = ntQuerySeg.getNucleotidesSubsequence(refStart, refEnd);
						return new ReferenceDifferenceNote(refStart, refEnd, refNts, queryNts, false, null);
					}
		}));
	}



	private List<ReferenceDifferenceNote> generateAaDifferenceNotes(
			List<AaReferenceSegment> aaReferenceSegments,
			List<AaReferenceSegment> aaQueryAlignedSegments, 
			Map<Integer, List<VariationNote>> refStartToVariationNotes) {
		return ReferenceSegment.intersection(aaReferenceSegments, aaQueryAlignedSegments, 
				new BiFunction<AaReferenceSegment, AaReferenceSegment, ReferenceDifferenceNote>() {
					@Override
					public ReferenceDifferenceNote apply(AaReferenceSegment aaRefSeg, AaReferenceSegment aaQuerySeg) {
						int refStart = Math.max(aaRefSeg.getRefStart(), aaQuerySeg.getRefStart());
						int refEnd = Math.min(aaRefSeg.getRefEnd(), aaQuerySeg.getRefEnd());
						CharSequence refAas = aaRefSeg.getAminoAcidsSubsequence(refStart, refEnd);
						CharSequence queryAas = aaQuerySeg.getAminoAcidsSubsequence(refStart, refEnd);
						return new ReferenceDifferenceNote(refStart, refEnd, refAas, queryAas, featureTreeResult.isIncludedInSummary(), refStartToVariationNotes);
					}

		});
	}



	
	public void deriveTranslationFromOrfAncestor(
			SequenceFeatureResult orfAncestorSequenceFeatureResult) {
		Integer codon1Start = featureTreeResult.getCodon1Start();

		ReferenceFeatureTreeResult orfAncestorFeatureTreeResult = orfAncestorSequenceFeatureResult.featureTreeResult;
		Integer orfAncestorCodon1Start = orfAncestorFeatureTreeResult.getCodon1Start();
		List<AaReferenceSegment> ancestorAaRefSegs = orfAncestorSequenceFeatureResult.aaQueryAlignedSegments;
		Integer ancestorReferenceToQueryOffset = orfAncestorSequenceFeatureResult.ntQueryAlignedSegments.get(0).getReferenceToQueryOffset();
		
		translateNtQuerySegsToAaQuerySegs(
				ancestorAaRefSegs, orfAncestorCodon1Start, ancestorReferenceToQueryOffset);

		List<AaMinorityVariant> orfAncestorAaMvs = orfAncestorSequenceFeatureResult.aaMinorityVariants;
		// add all minority variants which are located in this feature.
		for(AaMinorityVariant orfAncestorAaMv: orfAncestorAaMvs) {			
			if(ReferenceSegment.coversLocation(aaQueryAlignedSegments, orfAncestorAaMv.getAaIndex())) {
				aaMinorityVariants.add(orfAncestorAaMv.clone());
			}
		}
		
		// if necessary translate to feature's own codon coordinates.
		if(codon1Start != null) {
			int ntOffset = orfAncestorCodon1Start-codon1Start;
			int ancestorToLocalCodonOffset = ntOffset/3;
			aaQueryAlignedSegments.forEach(aaRefSeg -> aaRefSeg.translate(ancestorToLocalCodonOffset));
			aaMinorityVariants.forEach(aaMv -> aaMv.translate(ancestorToLocalCodonOffset));
		}
		
		
		
	}

	public void translateDirectly(boolean isORF, AbstractSequenceObject seqObj, CommandContext cmdContext) {
		int firstNtQuerySegRefStart = ntQueryAlignedSegments.get(0).getRefStart();
		int firstNtRefSegStart = featureTreeResult.getNtReferenceSegments().get(0).getRefStart();
		if(isORF && firstNtQuerySegRefStart != firstNtRefSegStart) {
			// query segments fail to cover the start codon, so we can't transcribe.
			return;
		} else {
			int firstQuerySegNtStart = ntQueryAlignedSegments.get(0).getQueryStart();
			int lastQuerySegNtEnd = ntQueryAlignedSegments.get(ntQueryAlignedSegments.size()-1).getQueryEnd();

			// attempt to transcribe everything between the start and end points.
			// the point of this is to pick up any possible stop codons or gaps in the gaps between aligned
			// segments
			CharSequence seqFeatureNts = seqObj.getNucleotides(cmdContext, firstQuerySegNtStart, lastQuerySegNtEnd);
			List<NtMinorityVariant> ntMinorityVariantsInFeature = 
					findMinorityVariantsInFeature(ntMinorityVariants, firstQuerySegNtStart, lastQuerySegNtEnd);
			
			boolean requireMethionineAtStart = true;
			if(!isORF) {
				requireMethionineAtStart = false;
			}
			boolean translateBeyondPossibleStop = cmdContext.getProjectSettingValue(ProjectSettingOption.TRANSLATE_BEYOND_POSSIBLE_STOP).equals("true");
			
			String seqFeatureAAs = TranslationUtils.translate(seqFeatureNts, requireMethionineAtStart, false, translateBeyondPossibleStop);
			if(seqFeatureAAs.length() != 0) {
				int firstSegRefToQueryOffset = ntQueryAlignedSegments.get(0).getReferenceToQueryOffset();
				for(NtQueryAlignedSegment ntQuerySeg : ntQueryAlignedSegments) {
					int segReferenceToQueryOffset = ntQuerySeg.getReferenceToQueryOffset();
					if( (segReferenceToQueryOffset - firstSegRefToQueryOffset) % 3 != 0 ) {
						continue; // skip any query segments which change the reading frame.
					}
					int segQueryToReferenceOffset = ntQuerySeg.getQueryToReferenceOffset();

					// conservatively select the transcribed chunk which is fully covered by this segment.
					int querySegNtStart = ntQuerySeg.getQueryStart();
					
					// find codon of this query seg within transcribed segment.
					int firstQuerySegCodon = TranslationUtils.getCodon(firstQuerySegNtStart, querySegNtStart);
					if(!TranslationUtils.isAtStartOfCodon(firstQuerySegNtStart, querySegNtStart)) {
						firstQuerySegCodon++;
					}
					int querySegNtEnd = ntQuerySeg.getQueryEnd();
					int lastQuerySegCodon = TranslationUtils.getCodon(firstQuerySegNtStart, querySegNtEnd);
					if(!TranslationUtils.isAtEndOfCodon(firstQuerySegNtStart, querySegNtEnd)) {
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
					int refNTStart = TranslationUtils.getNt(firstQuerySegNtStart, firstQuerySegCodon) + 
							segQueryToReferenceOffset;
					int codon1Start = featureTreeResult.getCodon1Start();
					
					int refCodonStart = TranslationUtils.getCodon(codon1Start, refNTStart);
					int refCodonEnd = refCodonStart+(querySegAas.length()-1);
					AaReferenceSegment aaSeg = new AaReferenceSegment(refCodonStart, refCodonEnd, querySegAas);
					aaQueryAlignedSegments.add(aaSeg);
					
					
					for(NtMinorityVariant ntMinorityVariant: ntMinorityVariantsInFeature) {
						int mvNtQueryIndex = ntMinorityVariant.getNtIndex();
						// translate to reference coordinate
						int mvNtRefIndex = mvNtQueryIndex+segQueryToReferenceOffset;
						// find the codon which contains the minority variant location.
						int mvCodonNumber = TranslationUtils.getCodon(codon1Start, mvNtRefIndex);
						
						if(mvCodonNumber >= refCodonStart && mvCodonNumber <= refCodonEnd) {
							// find the reference NT index where this codon starts
							int mvCodonNtRefStart = TranslationUtils.getNt(codon1Start, mvCodonNumber);
							// translate back to Query NT coordinates
							int mvCodonNtQueryStart = mvCodonNtRefStart + segReferenceToQueryOffset;
							// find the three NTs at this location in the sequence.
							char[] mvCodonNts = seqObj.getNucleotides(cmdContext, mvCodonNtQueryStart, mvCodonNtQueryStart+2).toString().toCharArray();
							// substitute in the minority variant NT
							mvCodonNts[mvNtQueryIndex-mvCodonNtQueryStart] = ntMinorityVariant.getNtValue();
							char mvAaValue = TranslationUtils.translate(mvCodonNts);
							if(mvAaValue != 0) {
								char nonVariantValue = aaSeg.getAminoAcidsSubsequence(mvCodonNumber, mvCodonNumber).charAt(0);
								if(mvAaValue != nonVariantValue) {
									aaMinorityVariants.add(
											new AaMinorityVariant(mvCodonNumber, mvAaValue, ntMinorityVariant.getProportion()));
								}
							}
						}
					}
				}
			}
		}
	}

	private List<NtMinorityVariant> findMinorityVariantsInFeature(
			List<NtMinorityVariant> ntMinorityVariants, int queryNtStart, int queryNtEnd) {
		List<NtMinorityVariant> ntMinorityVariantsInSegment = new ArrayList<NtMinorityVariant>();
		for(NtMinorityVariant ntMinorityVariant: ntMinorityVariants) {
			int ntMinorityVariantIndex = ntMinorityVariant.getNtIndex();
			if(ntMinorityVariantIndex >= queryNtStart && ntMinorityVariantIndex <= queryNtEnd) {
				ntMinorityVariantsInSegment.add(ntMinorityVariant);
			}
		}
		return ntMinorityVariantsInSegment;
	}

	// Given a list of aa segments covering the transcribed area, and a codon 1 start location
	// narrow this down to the set of aa segments for those reference regions covered by ntQueryAlignedSegments.
	// referenceToQueryOffset implicitly specifies the reading frame -- ntQueryAlignedSegments whose offset implies 
	// a different reading frame must be filtered out.
	private void translateNtQuerySegsToAaQuerySegs(
			List<AaReferenceSegment> aaReferenceSegments,
			Integer codon1Start,
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
				TranslationUtils.translateToCodonCoordinates(codon1Start, filteredNtSegments);
		// intersect this template with the already transcribed segment to get the final Query AA segments.
		aaQueryAlignedSegments.addAll(ReferenceSegment.intersection(templateAaRefSegs, aaReferenceSegments, 
				(templateSeg, aaRefSeg) -> {
					int refStart = Math.max(templateSeg.getRefStart(), aaRefSeg.getRefStart());
					int refEnd = Math.min(templateSeg.getRefEnd(), aaRefSeg.getRefEnd());
					CharSequence aminoAcids = aaRefSeg.getAminoAcidsSubsequence(refStart, refEnd);
					return new AaReferenceSegment(refStart, refEnd, aminoAcids);
				}
		));
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
		ArrayBuilder ntVariationNoteArray = seqFeatureResultObj.setArray("ntVariationNote");
		for(VariationNote ntVariationNote: ntVariationNotes) {
			ntVariationNote.toDocument(ntVariationNoteArray.addObject());
		}
		ArrayBuilder ntRefDiffArray = seqFeatureResultObj.setArray("ntReferenceDifferenceNote");
		for(ReferenceDifferenceNote ntRefDiff: ntReferenceDifferenceNotes) {
			ntRefDiff.toDocument(ntRefDiffArray.addObject());
		}
		ArrayBuilder ntMinorityVariantArray = seqFeatureResultObj.setArray("ntMinorityVariant");
		for(NtMinorityVariant ntMinorityVariant: ntMinorityVariants) {
			ntMinorityVariant.toDocument(ntMinorityVariantArray.addObject());
		}

		if(aaVariationNotes != null) {
			ArrayBuilder aaVariationNoteArray = seqFeatureResultObj.setArray("aaVariationNote");
			for(VariationNote aaVariationNote: aaVariationNotes) {
				aaVariationNote.toDocument(aaVariationNoteArray.addObject());
			}
		}
		if(aaReferenceDifferenceNotes != null) {
			ArrayBuilder aaRefDiffArray = seqFeatureResultObj.setArray("aaReferenceDifferenceNote");
			for(ReferenceDifferenceNote aaRefDiff: aaReferenceDifferenceNotes) {
				aaRefDiff.toDocument(aaRefDiffArray.addObject());
			}
		}
		if(aaMinorityVariants != null) {
			ArrayBuilder aaMinorityVariantArray = seqFeatureResultObj.setArray("aaMinorityVariant");
			for(AaMinorityVariant aaMinorityVariant: aaMinorityVariants) {
				aaMinorityVariant.toDocument(aaMinorityVariantArray.addObject());
			}
		}
	}

	public List<AaReferenceSegment> getAaQueryAlignedSegments() {
		return aaQueryAlignedSegments;
	}

	public List<ReferenceDifferenceNote> getAaReferenceDifferenceNotes() {
		return aaReferenceDifferenceNotes;
	}

	public List<VariationNote> getAaVariationNotes() {
		return aaVariationNotes;
	}

	
	
	
}