package uk.ac.gla.cvr.gluetools.core.datamodel.auto;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

/**
 * Class _ProjectSetting was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _ProjectSetting extends GlueDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String VALUE_PROPERTY = "value";

    public static final String NAME_PK_COLUMN = "NAME";

    public void setName(String name) {
        writeProperty(NAME_PROPERTY, name);
    }
    public String getName() {
        return (String)readProperty(NAME_PROPERTY);
    }

    public void setValue(String value) {
        writeProperty(VALUE_PROPERTY, value);
    }
    public String getValue() {
        return (String)readProperty(VALUE_PROPERTY);
    }

}