package uk.ac.gla.cvr.gluetools.core.datamodel.auto;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;

/**
 * Class _MemberFLocNote was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _MemberFLocNote extends GlueDataObject {

    public static final String FEATURE_LOC_PROPERTY = "featureLoc";
    public static final String MEMBER_PROPERTY = "member";

    public static final String ALIGNMENT_NAME_PK_COLUMN = "alignment_name";
    public static final String FEATURE_NAME_PK_COLUMN = "feature_name";
    public static final String REF_SEQ_NAME_PK_COLUMN = "ref_seq_name";
    public static final String SEQUENCE_ID_PK_COLUMN = "sequence_id";
    public static final String SOURCE_NAME_PK_COLUMN = "source_name";

    public void setFeatureLoc(FeatureLocation featureLoc) {
        setToOneTarget(FEATURE_LOC_PROPERTY, featureLoc, true);
    }

    public FeatureLocation getFeatureLoc() {
        return (FeatureLocation)readProperty(FEATURE_LOC_PROPERTY);
    }


    public void setMember(AlignmentMember member) {
        setToOneTarget(MEMBER_PROPERTY, member, true);
    }

    public AlignmentMember getMember() {
        return (AlignmentMember)readProperty(MEMBER_PROPERTY);
    }


}