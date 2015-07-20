package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public class CreateResult extends OkCrudResult {

	public CreateResult(Class<? extends GlueDataObject> objectClass, int number) {
		super(Operation.CREATE, objectClass, number);
	}

}
