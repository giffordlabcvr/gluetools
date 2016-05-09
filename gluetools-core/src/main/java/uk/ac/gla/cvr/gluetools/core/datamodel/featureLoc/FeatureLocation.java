package uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.CodonLabeler;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueConfigContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.segments.AaReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationFormat;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;


@GlueDataClass(defaultListedProperties = {FeatureLocation.FEATURE_NAME_PATH})
public class FeatureLocation extends _FeatureLocation {
	
	public static final String REF_SEQ_NAME_PATH = 
			_FeatureLocation.REFERENCE_SEQUENCE_PROPERTY+"."+_ReferenceSequence.NAME_PROPERTY;
	public static final String FEATURE_NAME_PATH = 
			_FeatureLocation.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;

	
	public static Map<String, String> pkMap(String referenceSequenceName, String featureName) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(REF_SEQ_NAME_PATH, referenceSequenceName);
		idMap.put(FEATURE_NAME_PATH, featureName);
		return idMap;
	}

	
	private List<LabeledCodon> labeledCodons;
	private TIntObjectMap<LabeledCodon> refNtToLabeledCodon;
	private Map<String, LabeledCodon> labelToLabeledCodon;
	
	
	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}


	public List<LabeledCodon> getLabeledCodons(CommandContext cmdContext) {
		Feature feature = getFeature();
		feature.checkCodesAminoAcids();
		if(labeledCodons == null) {
			CodonLabeler codonLabeler = feature.getCodonLabelerModule(cmdContext);
			if(codonLabeler == null) {
				Integer codon1Start = getCodon1Start(cmdContext);
				Integer ntStart = ReferenceSegment.minRefStart(getSegments());
				Integer ntEnd = ReferenceSegment.maxRefEnd(getSegments());
				// default case: no labeler module.
				labeledCodons = new ArrayList<LabeledCodon>();
				for(int i = ntStart; i <= ntEnd; i++) {
					if(TranslationUtils.isAtStartOfCodon(codon1Start, i)) {
						labeledCodons.add(new LabeledCodon(Integer.toString(TranslationUtils.getCodon(codon1Start, i)), i));
					}
				}
			} else {
				labeledCodons = codonLabeler.labelCodons(cmdContext, this);
			}
		}
		return labeledCodons;
	}
	
	public TIntObjectMap<LabeledCodon> getRefNtToLabeledCodon(CommandContext cmdContext) {
		if(refNtToLabeledCodon == null) {
			refNtToLabeledCodon = new TIntObjectHashMap<LabeledCodon>();
			List<LabeledCodon> labeledCodons = getLabeledCodons(cmdContext);
			for(LabeledCodon labeledCodon: labeledCodons) {
				refNtToLabeledCodon.put(labeledCodon.getNtStart(), labeledCodon);
			}
		}
		return refNtToLabeledCodon;
	}
	
	public Map<String, LabeledCodon> getLabelToLabeledCodon(CommandContext cmdContext) {
		if(labelToLabeledCodon == null) {
			labelToLabeledCodon = new LinkedHashMap<String, LabeledCodon>();
			List<LabeledCodon> labeledCodons = getLabeledCodons(cmdContext);
			for(LabeledCodon labeledCodon: labeledCodons) {
				labelToLabeledCodon.put(labeledCodon.getCodonLabel(), labeledCodon);
			}
		}
		return labelToLabeledCodon;
	}
	
	public LabeledCodon getFirstLabeledCodon(CommandContext cmdContext) {
		return getLabeledCodons(cmdContext).get(0);
	}

	public LabeledCodon getLastLabeledCodon(CommandContext cmdContext) {
		List<LabeledCodon> labeledCodons = getLabeledCodons(cmdContext);
		return labeledCodons.get(labeledCodons.size() - 1);
	}
	
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(getReferenceSequence().getName(), getFeature().getName());
	}
	
	public FeatureLocation getNextAncestorLocation(CommandContext cmdContext) {
		Feature feature = getFeature();
		Feature nextAncestorFeature = feature.getNextAncestor();
		if(nextAncestorFeature != null) {
			return GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
					FeatureLocation.pkMap(getReferenceSequence().getName(), nextAncestorFeature.getName()), true);
		}
		return null;
		
	}
	
	public FeatureLocation getCodonNumberingAncestorLocation(CommandContext cmdContext) {
		if(getFeature().hasOwnCodonNumbering()) {
			return this;
		}
		FeatureLocation nextAncestor = getNextAncestorLocation(cmdContext);
		if(nextAncestor == null) {
			return null;
		}
		return nextAncestor.getCodonNumberingAncestorLocation(cmdContext);
	}

	
	public FeatureLocation getOrfAncestorLocation(CommandContext cmdContext) {
		if(getFeature().isOpenReadingFrame()) {
			return this;
		}
		FeatureLocation nextAncestor = getNextAncestorLocation(cmdContext);
		if(nextAncestor == null) {
			return null;
		}
		return nextAncestor.getOrfAncestorLocation(cmdContext);
	}
	
	public void validate(CommandContext cmdContext) {
		List<FeatureSegment> segments = getSegments();
		Feature feature = getFeature();
		if(segments.isEmpty()) {
			throw new FeatureLocationException(FeatureLocationException.Code.FEATURE_LOCATION_HAS_NO_SEGMENTS, 
					getReferenceSequence().getName(), feature.getName());
		}
		Feature nextAncestorFeature = feature.getNextAncestor();
		FeatureLocation nextAncestorFeatureLocation = getNextAncestorLocation(cmdContext);
		if(nextAncestorFeature != null && nextAncestorFeatureLocation == null) {
			throw new FeatureLocationException(FeatureLocationException.Code.NEXT_ANCESTOR_FEATURE_LOCATION_UNDEFINED, 
					getReferenceSequence().getName(), feature.getName(), nextAncestorFeature.getName());
		}
		if(nextAncestorFeatureLocation != null) {
			if(!ReferenceSegment.covers(nextAncestorFeatureLocation.getSegments(), segments)) {
				throw new FeatureLocationException(FeatureLocationException.Code.FEATURE_LOCATION_NOT_CONTAINED_WITHIN_NEXT_ANCESTOR, 
						getReferenceSequence().getName(), feature.getName(), nextAncestorFeature.getName());
			}
		}
		Integer codon1Start = getCodon1Start(cmdContext);
		if(codon1Start != null) { // might be undefined if there is a valiation problem elsewhere
			int codon1Int = codon1Start.intValue();
			segments.forEach(seg -> {
				checkCodonAligned(feature, codon1Int, seg);
			});
		}
		getVariations().forEach(variation -> variation.validate(cmdContext));
		
	}

	private void checkCodonAligned(Feature feature, int codon1Start, IReferenceSegment seg) {
		if(!TranslationUtils.isAtStartOfCodon(codon1Start, seg.getRefStart()) || 
				!TranslationUtils.isAtEndOfCodon(codon1Start, seg.getRefEnd())) {
			throw new FeatureLocationException(
					FeatureLocationException.Code.FEATURE_LOCATION_SEGMENT_NOT_CODON_ALIGNED, 
					getReferenceSequence().getName(), feature.getName(), Integer.toString(seg.getRefStart()), 
					Integer.toString(seg.getRefEnd()), Integer.toString(codon1Start));
		}
	}
	
	/**
	 * Returns the reference NT number which points to codon 1 in the codon numbering system applicable to this
	 * feature location.
	 * @param cmdContext
	 * @return
	 */
	public Integer getCodon1Start(CommandContext cmdContext) {
		FeatureLocation codonNumberingAncestorLocation = getCodonNumberingAncestorLocation(cmdContext);
		if(codonNumberingAncestorLocation == null) {
			return null;
		}
		List<FeatureSegment> segments = codonNumberingAncestorLocation.getSegments();
		if(!segments.isEmpty()) {
			// first segment establishes codon1start
			return segments.get(0).getRefStart();
		}
		return null;
	}

	/**
	 *  Returns the highest-numbered codon in the feature-location's own codon numbering. 
	 *  This is null unless the following are true of the feature-location
	 * -- its feature has the OWN_CODON_NUMBERING metatag 
	 * -- it has a non-empty list of segments 
	 **/
	public Integer getOwnCodonNumberingMax(CommandContext cmdContext) {
		if(!getFeature().hasOwnCodonNumbering()) {
			return null;
		}
		List<FeatureSegment> segments = getSegments();
		if(segments.isEmpty()) {
			return null;
		}
		int codons = 0;
		for(FeatureSegment segment: segments) {
			codons += segment.getCurrentLength() / 3;
		}
		return codons;
	}
	
	/**
	 * Translate an ORF feature location to AAs
	 * Returns null if this feature location is not an ORF.
	 * @param cmdContext
	 * @return
	 */
	public List<AaReferenceSegment> translate(CommandContext cmdContext) {
		Feature feature = getFeature();
		if(feature.isOpenReadingFrame()) {
			Integer codon1Start = getCodon1Start(cmdContext);
			if(codon1Start == null) {
				return null;
			}
			List<FeatureSegment> featureLocSegments = getSegments();
			if(featureLocSegments.isEmpty()) {
				return null;
			}
			List<ReferenceSegment> segmentsToTranslate = featureLocSegments.stream()
				.map(featureLocSeg -> new ReferenceSegment(featureLocSeg.getRefStart(), featureLocSeg.getRefEnd()))
				.collect(Collectors.toList());

			ReferenceSequence refSeq = getReferenceSequence();
			List<NtReferenceSegment> ntSegmentsToTranscribe = 
					refSeq.getSequence().getSequenceObject().getNtReferenceSegments(segmentsToTranslate, cmdContext);
			
			List<AaReferenceSegment> transcribedSegments = new ArrayList<AaReferenceSegment>();
			
			for(NtReferenceSegment ntSegment : ntSegmentsToTranscribe) {
				checkCodonAligned(feature, codon1Start, ntSegment);
				CharSequence nucleotides = ntSegment.getNucleotides();
				boolean translateBeyondPossibleStop = cmdContext.getProjectSettingValue(ProjectSettingOption.TRANSLATE_BEYOND_POSSIBLE_STOP).equals("true");
				boolean translateBeyondDefiniteStop = cmdContext.getProjectSettingValue(ProjectSettingOption.TRANSLATE_BEYOND_DEFINITE_STOP).equals("true");
				String aminoAcids = TranslationUtils.translate(nucleotides, true, false, translateBeyondPossibleStop, translateBeyondDefiniteStop);
				if(aminoAcids.length() != nucleotides.length() / 3) {
					throw new FeatureLocationException(
							FeatureLocationException.Code.FEATURE_LOCATION_ORF_TRANSCRIPTION_INCOMPLETE, 
							refSeq.getName(), feature.getName(), ntSegment.getRefStart(), ntSegment.getRefEnd());
				}
				int aaRefStart = TranslationUtils.getCodon(codon1Start, ntSegment.getRefStart());
				int aaRefEnd = aaRefStart+(aminoAcids.length()-1);
				transcribedSegments.add(new AaReferenceSegment(aaRefStart, aaRefEnd, aminoAcids));
			};
			return transcribedSegments;
		}
		return null;
	}

	@Override
	public void generateGlueConfig(int indent, StringBuffer glueConfigBuf, GlueConfigContext glueConfigContext) {
		String noCommit = glueConfigContext.getNoCommit() ? "--noCommit " : "";
		if(glueConfigContext.getIncludeVariations()) {
			for(Variation variation: getVariations()) {
				indent(glueConfigBuf, indent).append("create variation "+noCommit).append(variation.getName())
					.append(" -t ").append(variation.getTranslationType());
				String description = variation.getDescription();
				if(description != null) {
					glueConfigBuf.append(" \""+description+"\"");
				}
				glueConfigBuf.append("\n");

				StringBuffer variationConfigBuf = new StringBuffer();
				variation.generateGlueConfig(indent+INDENT, variationConfigBuf, glueConfigContext);
				if(variationConfigBuf.length() > 0) {
					indent(glueConfigBuf, indent).append("variation ").append(variation.getName()).append("\n");
					glueConfigBuf.append(variationConfigBuf.toString());
					indent(glueConfigBuf, indent+INDENT).append("exit\n");
				}
			}
		}
	}

	
	public List<ReferenceSegment> segmentsAsReferenceSegments() {
		return getSegments().stream().map(seg -> seg.asReferenceSegment()).collect(Collectors.toList());
	}


	public List<VariationScanResult> variationScan(
			CommandContext cmdContext,
			List<NtQueryAlignedSegment> queryToFeatureLocRefNtSegs, List<Variation> variationsToScan, boolean excludeAbsent) {
		Translator translator = null;
		Integer codon1Start = null;
		if(getFeature().codesAminoAcids()) {
			translator = new CommandContextTranslator(cmdContext);
			codon1Start = getCodon1Start(cmdContext);
		}
		List<VariationScanResult> variationScanResults = new ArrayList<VariationScanResult>();
		
		List<NtQueryAlignedSegment> queryToFeatureLocRefNtSegsMerged = 
				ReferenceSegment.mergeAbutting(queryToFeatureLocRefNtSegs, 
						NtQueryAlignedSegment.ntMergeAbuttingFunction(), 
						QueryAlignedSegment.abutsPredicate());
		
		for(NtQueryAlignedSegment ntQaSeg: queryToFeatureLocRefNtSegsMerged) {
			variationScanResults.addAll(variationScanSegment(translator, codon1Start, ntQaSeg, variationsToScan, excludeAbsent));
		}
		return variationScanResults;
	}

	public List<Variation> getVariationsQualified(CommandContext cmdContext, Expression variationWhereClauseExtra) {
		Expression variationWhereClause = 
				ExpressionFactory.matchExp(Variation.FEATURE_NAME_PATH, getFeature().getName())
				.andExp(ExpressionFactory.matchExp(Variation.REF_SEQ_NAME_PATH, getReferenceSequence().getName()));

		if(variationWhereClauseExtra != null) {
			variationWhereClause = variationWhereClause.andExp(variationWhereClauseExtra);
		}
		SelectQuery query = new SelectQuery(Variation.class, variationWhereClause);
		query.addOrdering(Variation.REF_START_PROPERTY, SortOrder.ASCENDING);
		query.addOrdering(Variation.NAME_PROPERTY, SortOrder.ASCENDING);
		return GlueDataObject.query(cmdContext, Variation.class, query);
	}
	
	public List<VariationScanResult> variationScanSegment(Translator translator, Integer codon1Start,
			NtQueryAlignedSegment ntQaSeg, List<Variation> variationsToScan, boolean excludeAbsent) {
		List<VariationScanResult> variationScanResults = new ArrayList<VariationScanResult>();
		
		String fullProteinTranslation = null;
		NtQueryAlignedSegment ntQaSegCdnAligned = null;
		if(getFeature().codesAminoAcids()) {
			List<NtQueryAlignedSegment> ntQaSegsCdnAligned = TranslationUtils.truncateToCodonAligned(codon1Start, Arrays.asList(ntQaSeg));
			if(ntQaSegsCdnAligned.isEmpty()) {
				fullProteinTranslation = "";
			} else {
				ntQaSegCdnAligned = ntQaSegsCdnAligned.get(0);
				fullProteinTranslation = translator.translate(ntQaSegCdnAligned.getNucleotides());
			}
		}
		
		for(Variation variationToScan: variationsToScan) {
			Integer refStart = variationToScan.getRefStart();
			Integer refEnd = variationToScan.getRefEnd();
			int varLengthNt = refEnd - refStart + 1;
			if(variationToScan.getTranslationFormat() == TranslationFormat.AMINO_ACID) {
				Integer proteinTranslationRefNtStart = ntQaSegCdnAligned.getRefStart();
				Integer proteinTranslationRefNtEnd = ntQaSegCdnAligned.getRefEnd();
				if(!( refStart >= proteinTranslationRefNtStart && refEnd <= proteinTranslationRefNtEnd )) {
					continue;
				}
				int segToVariationStartOffset = refStart - proteinTranslationRefNtStart;
				int startAA = segToVariationStartOffset / 3;
				int endAA = startAA + ( (varLengthNt / 3) - 1);
				CharSequence proteinTranslationForVariation = fullProteinTranslation.subSequence(startAA, endAA+1);
				int scanQueryNtStart = ntQaSegCdnAligned.getQueryStart() + segToVariationStartOffset;
				VariationScanResult proteinScanResult = variationToScan.scanProteinTranslation(proteinTranslationForVariation, scanQueryNtStart);
				if(proteinScanResult.isPresent() || !excludeAbsent) {
					variationScanResults.add(proteinScanResult);
				}
			} else if(variationToScan.getTranslationFormat() == TranslationFormat.NUCLEOTIDE) {
				if(!( refStart >= ntQaSeg.getRefStart() && refEnd <= ntQaSeg.getRefEnd() )) {
					continue;
				}
				ReferenceSegment variationRegionSeg = new ReferenceSegment(refStart, refEnd);
				List<NtQueryAlignedSegment> intersection = ReferenceSegment.intersection(Arrays.asList(ntQaSeg), Arrays.asList(variationRegionSeg), 
						ReferenceSegment.cloneLeftSegMerger());
				if(intersection.isEmpty()) {
					continue;
				}
				NtQueryAlignedSegment intersectionSeg = intersection.get(0);
				CharSequence nucleotides = intersectionSeg.getNucleotides();
				VariationScanResult ntScanResult = variationToScan.scanNucleotides(nucleotides, intersectionSeg.getQueryStart());
				if(ntScanResult.isPresent() || !excludeAbsent) {
					variationScanResults.add(ntScanResult);
				}
			}
		}
		
		return variationScanResults;
	}
}

