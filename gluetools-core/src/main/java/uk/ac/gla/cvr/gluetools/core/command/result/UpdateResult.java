package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public class UpdateResult extends OkCrudResult {

	public UpdateResult(Class<? extends GlueDataObject> objectClass, int number) {
		super(Operation.UPDATE, objectClass, number);
	}

}
