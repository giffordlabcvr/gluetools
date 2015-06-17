package uk.ac.gla.cvr.gluetools.core.command;

import org.apache.cayenne.ObjectId;

public class CreateCommandResult extends CommandResult {

	private ObjectId objectId;

	public CreateCommandResult(ObjectId objectId) {
		super();
		this.objectId = objectId;
	}

	public ObjectId getObjectId() {
		return objectId;
	}
	
}
