package uk.ac.gla.cvr.gluetools.core.datamodel.memberFLocNote;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._MemberFLocNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;

@GlueDataClass(
		defaultListedProperties = { MemberFLocNote.ALIGNMENT_NAME_PATH, MemberFLocNote.SOURCE_NAME_PATH, MemberFLocNote.SEQUENCE_ID_PATH, MemberFLocNote.REF_SEQ_NAME_PATH, MemberFLocNote.FEATURE_NAME_PATH },
		listableBuiltInProperties = { MemberFLocNote.ALIGNMENT_NAME_PATH, MemberFLocNote.SOURCE_NAME_PATH, MemberFLocNote.SEQUENCE_ID_PATH, MemberFLocNote.REF_SEQ_NAME_PATH, MemberFLocNote.FEATURE_NAME_PATH },
		modifiableBuiltInProperties = { })		
public class MemberFLocNote extends _MemberFLocNote {

	public static final String ALIGNMENT_NAME_PATH = _MemberFLocNote.MEMBER_PROPERTY+"."+AlignmentMember.ALIGNMENT_NAME_PATH;
	public static final String SOURCE_NAME_PATH = _MemberFLocNote.MEMBER_PROPERTY+"."+AlignmentMember.SOURCE_NAME_PATH;
	public static final String SEQUENCE_ID_PATH = _MemberFLocNote.MEMBER_PROPERTY+"."+AlignmentMember.SEQUENCE_ID_PATH;
	public static final String REF_SEQ_NAME_PATH = _MemberFLocNote.FEATURE_LOC_PROPERTY+"."+FeatureLocation.REF_SEQ_NAME_PATH;
	public static final String FEATURE_NAME_PATH  = _MemberFLocNote.FEATURE_LOC_PROPERTY+"."+FeatureLocation.FEATURE_NAME_PATH;

	public static Map<String, String> pkMap(
			String alignmentName, 
			String sourceName, 
			String sequenceID, 
			String referenceName, 
			String featureName) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(ALIGNMENT_NAME_PATH, alignmentName);
		idMap.put(SOURCE_NAME_PATH, sourceName);
		idMap.put(SEQUENCE_ID_PATH, sequenceID);
		idMap.put(REF_SEQ_NAME_PATH, referenceName);
		idMap.put(FEATURE_NAME_PATH, featureName);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getMember().getAlignment().getName(), 
				getMember().getSequence().getSource().getName(),
				getMember().getSequence().getSequenceID(),
				getFeatureLoc().getReferenceSequence().getName(), 
				getFeatureLoc().getFeature().getName());
	}

	
}
