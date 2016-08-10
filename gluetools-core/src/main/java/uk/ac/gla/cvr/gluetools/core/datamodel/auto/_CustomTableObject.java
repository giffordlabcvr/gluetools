package uk.ac.gla.cvr.gluetools.core.datamodel.auto;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;

public abstract class _CustomTableObject extends GlueDataObject {

    public static final String ID_PROPERTY = "id";

    public static final String ID_PK_COLUMN = "id";

    public void setId(String id) {
        writeProperty(ID_PROPERTY, id);
    }
    public String getId() {
        return (String)readProperty(ID_PROPERTY);
    }

}
