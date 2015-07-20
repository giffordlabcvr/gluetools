package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public class DeleteResult extends OkCrudResult {

	public DeleteResult(Class<? extends GlueDataObject> objectClass, int number) {
		super(Operation.DELETE, objectClass, number);
	}

}
