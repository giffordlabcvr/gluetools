package uk.ac.gla.cvr.gluetools.core.command.result;


public class UpdateResult extends OkCrudResult {

	public UpdateResult(Class<?> objectClass, int number) {
		super(Operation.UPDATE, objectClass, number);
	}

}
