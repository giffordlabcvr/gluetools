package uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import uk.ac.gla.cvr.gluetools.core.segments.AaReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;


@GlueDataClass(defaultListColumns = {FeatureLocation.FEATURE_NAME_PATH})
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

	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}

	
	@Override
	protected Map<String, String> pkMap() {
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
				String aminoAcids = TranslationUtils.translate(nucleotides, true, false, translateBeyondPossibleStop);
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
		if(glueConfigContext.includeVariations()) {
			for(Variation variation: getVariations()) {
				indent(glueConfigBuf, indent).append("create variation ").append(variation.getName());
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


}

