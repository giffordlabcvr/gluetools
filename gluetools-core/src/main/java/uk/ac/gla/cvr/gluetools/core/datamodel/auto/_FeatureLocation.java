package uk.ac.gla.cvr.gluetools.core.datamodel.auto;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

/**
 * Class _FeatureLocation was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _FeatureLocation extends GlueDataObject {

    public static final String FEATURE_PROPERTY = "feature";
    public static final String REFERENCE_SEQUENCE_PROPERTY = "referenceSequence";
    public static final String SEGMENTS_PROPERTY = "segments";

    public static final String FEATURE_PK_COLUMN = "FEATURE";
    public static final String REF_SEQUENCE_PK_COLUMN = "REF_SEQUENCE";

    public void setFeature(Feature feature) {
        setToOneTarget(FEATURE_PROPERTY, feature, true);
    }

    public Feature getFeature() {
        return (Feature)readProperty(FEATURE_PROPERTY);
    }


    public void setReferenceSequence(ReferenceSequence referenceSequence) {
        setToOneTarget(REFERENCE_SEQUENCE_PROPERTY, referenceSequence, true);
    }

    public ReferenceSequence getReferenceSequence() {
        return (ReferenceSequence)readProperty(REFERENCE_SEQUENCE_PROPERTY);
    }


    public void addToSegments(FeatureSegment obj) {
        addToManyTarget(SEGMENTS_PROPERTY, obj, true);
    }
    public void removeFromSegments(FeatureSegment obj) {
        removeToManyTarget(SEGMENTS_PROPERTY, obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<FeatureSegment> getSegments() {
        return (List<FeatureSegment>)readProperty(SEGMENTS_PROPERTY);
    }


}