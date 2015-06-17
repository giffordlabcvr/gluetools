package uk.ac.gla.cvr.gluetools.core.datamodel;

import org.apache.cayenne.CayenneDataObject;

public abstract class GlueDataObject extends CayenneDataObject {

	public abstract String[] populateListRow();
}
