package uk.ac.gla.cvr.gluetools.core.datamodel.auto;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;

/**
 * Class _VcatMembership was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _VcatMembership extends GlueDataObject {

    public static final String CATEGORY_PROPERTY = "category";
    public static final String VARIATION_PROPERTY = "variation";

    public static final String CATEGORY_NAME_PK_COLUMN = "CATEGORY_NAME";
    public static final String VARIATION_NAME_PK_COLUMN = "VARIATION_NAME";

    public void setCategory(VariationCategory category) {
        setToOneTarget(CATEGORY_PROPERTY, category, true);
    }

    public VariationCategory getCategory() {
        return (VariationCategory)readProperty(CATEGORY_PROPERTY);
    }


    public void setVariation(Variation variation) {
        setToOneTarget(VARIATION_PROPERTY, variation, true);
    }

    public Variation getVariation() {
        return (Variation)readProperty(VARIATION_PROPERTY);
    }


}