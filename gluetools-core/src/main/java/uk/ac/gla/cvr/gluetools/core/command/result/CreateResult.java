package uk.ac.gla.cvr.gluetools.core.command.result;


public class CreateResult extends OkCrudResult {

	public CreateResult(Class<?> objectClass, int number) {
		super(Operation.CREATE, objectClass, number);
	}

}
