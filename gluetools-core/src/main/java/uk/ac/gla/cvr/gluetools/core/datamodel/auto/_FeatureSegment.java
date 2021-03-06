package uk.ac.gla.cvr.gluetools.core.datamodel.auto;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;

/**
 * Class _FeatureSegment was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _FeatureSegment extends GlueDataObject {

    public static final String REF_END_PROPERTY = "refEnd";
    public static final String REF_START_PROPERTY = "refStart";
    public static final String SPLICE_INDEX_PROPERTY = "spliceIndex";
    public static final String TRANSCRIPTION_INDEX_PROPERTY = "transcriptionIndex";
    public static final String TRANSLATION_MODIFIER_NAME_PROPERTY = "translationModifierName";
    public static final String FEATURE_LOCATION_PROPERTY = "featureLocation";

    public static final String FEATURE_NAME_PK_COLUMN = "feature_name";
    public static final String REF_END_PK_COLUMN = "ref_end";
    public static final String REF_SEQ_NAME_PK_COLUMN = "ref_seq_name";
    public static final String REF_START_PK_COLUMN = "ref_start";

    public void setRefEnd(Integer refEnd) {
        writeProperty(REF_END_PROPERTY, refEnd);
    }
    public Integer getRefEnd() {
        return (Integer)readProperty(REF_END_PROPERTY);
    }

    public void setRefStart(Integer refStart) {
        writeProperty(REF_START_PROPERTY, refStart);
    }
    public Integer getRefStart() {
        return (Integer)readProperty(REF_START_PROPERTY);
    }

    public void setSpliceIndex(Integer spliceIndex) {
        writeProperty(SPLICE_INDEX_PROPERTY, spliceIndex);
    }
    public Integer getSpliceIndex() {
        return (Integer)readProperty(SPLICE_INDEX_PROPERTY);
    }

    public void setTranscriptionIndex(Integer transcriptionIndex) {
        writeProperty(TRANSCRIPTION_INDEX_PROPERTY, transcriptionIndex);
    }
    public Integer getTranscriptionIndex() {
        return (Integer)readProperty(TRANSCRIPTION_INDEX_PROPERTY);
    }

    public void setTranslationModifierName(String translationModifierName) {
        writeProperty(TRANSLATION_MODIFIER_NAME_PROPERTY, translationModifierName);
    }
    public String getTranslationModifierName() {
        return (String)readProperty(TRANSLATION_MODIFIER_NAME_PROPERTY);
    }

    public void setFeatureLocation(FeatureLocation featureLocation) {
        setToOneTarget(FEATURE_LOCATION_PROPERTY, featureLocation, true);
    }

    public FeatureLocation getFeatureLocation() {
        return (FeatureLocation)readProperty(FEATURE_LOCATION_PROPERTY);
    }


}
