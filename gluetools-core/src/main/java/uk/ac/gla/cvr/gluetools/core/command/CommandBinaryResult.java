package uk.ac.gla.cvr.gluetools.core.command;

import java.util.Base64;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;

public class CommandBinaryResult extends CommandResult {

	public CommandBinaryResult(String rootObjectName, byte[] bytes) {
		super(rootObjectName);
		String bytesAsBase64 = Base64.getEncoder().encodeToString(bytes);
		getCommandDocument().set(Command.BINARY_OUTPUT_PROPERTY, bytesAsBase64);
	}

}
