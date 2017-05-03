package uk.ac.gla.cvr.gluetools.core.featurePresenceRecorder;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.memberFLocNote.MemberFLocNote;

public class RecordFeaturePresenceResult extends BaseTableResult<MemberFeaturePresence> {

	public static final String REFERENCE_NT_COVERAGE = "referenceNtCoverage";

	public RecordFeaturePresenceResult(List<MemberFeaturePresence> rowObjects) {
		super("recordFeaturePresenceResult", rowObjects, 
				column(MemberFLocNote.ALIGNMENT_NAME_PATH, 
						mfp -> mfp.getMemberPkMap().get(AlignmentMember.ALIGNMENT_NAME_PATH)), 
				column(MemberFLocNote.SOURCE_NAME_PATH, 
						mfp -> mfp.getMemberPkMap().get(AlignmentMember.SOURCE_NAME_PATH)), 
				column(MemberFLocNote.SEQUENCE_ID_PATH, 
						mfp -> mfp.getMemberPkMap().get(AlignmentMember.SEQUENCE_ID_PATH)), 
				column(MemberFLocNote.REF_SEQ_NAME_PATH, 
						mfp -> mfp.getFeatureLocationPkMap().get(FeatureLocation.REF_SEQ_NAME_PATH)), 
				column(MemberFLocNote.FEATURE_NAME_PATH, 
						mfp -> mfp.getFeatureLocationPkMap().get(FeatureLocation.FEATURE_NAME_PATH)), 
				column(REFERENCE_NT_COVERAGE, 
						mfp -> mfp.getReferenceNtCoverage()));
	}

}
