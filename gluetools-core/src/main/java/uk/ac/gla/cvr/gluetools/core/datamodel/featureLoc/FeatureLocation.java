package uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;import uk.ac.gla.cvr.gluetools.core.transcription.TranscriptionUtils;


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
			return GlueDataObject.lookup(cmdContext.getObjectContext(), FeatureLocation.class, 
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
		FeatureLocation parentFeatureLocation = getNextAncestorLocation(cmdContext);
		if(parentFeatureLocation == null) {
			return null;
		}
		return parentFeatureLocation.getCodonNumberingAncestorLocation(cmdContext);
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
		if(feature.hasOwnCodonNumbering()) {
			Integer codon1Start = null;
			if(feature.isOpenReadingFrame()) {
				// first segment establishes codon1start
				codon1Start = segments.get(0).getRefStart();
			} else {
				// ORF ancestor location first segment establishes codon1start;
				FeatureLocation orfAncestorLocation = getOrfAncestorLocation(cmdContext);
				if(orfAncestorLocation != null && !orfAncestorLocation.getSegments().isEmpty()) {
					codon1Start = orfAncestorLocation.getSegments().get(0).getRefStart();
				}
			}
			if(codon1Start != null) { // might be undefined if there is a valiation problem elsewhere
				int codon1Int = codon1Start.intValue();
				segments.forEach(seg -> {
					if(!TranscriptionUtils.isAtStartOfCodon(codon1Int, seg.getRefStart()) || 
							!TranscriptionUtils.isAtEndOfCodon(codon1Int, seg.getRefEnd())) {
						throw new FeatureLocationException(
								FeatureLocationException.Code.FEATURE_LOCATION_SEGMENT_NOT_CODON_ALIGNED, 
								getReferenceSequence().getName(), feature.getName(), Integer.toString(seg.getRefStart()), 
								Integer.toString(seg.getRefEnd()), Integer.toString(codon1Int));
					}
				});
			}
		}
		getVariations().forEach(variation -> variation.validate(cmdContext));
		
	}

	// this is relevant only if the feature has the OWN_CODON_NUMBERING metatag.
	public Integer getMaxCodonNumber() {
		List<FeatureSegment> segments = getSegments();
		int codons = 0;
		for(FeatureSegment segment: segments) {
			codons += segment.getCurrentLength() / 3;
		}
		return codons;
	}
	
	
}

