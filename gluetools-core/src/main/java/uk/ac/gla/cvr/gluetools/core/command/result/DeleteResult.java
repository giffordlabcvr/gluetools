package uk.ac.gla.cvr.gluetools.core.command.result;


public class DeleteResult extends OkCrudResult {

	public DeleteResult(Class<?> objectClass, int number) {
		super(Operation.DELETE, objectClass, number);
	}

}
