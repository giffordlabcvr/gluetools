package uk.ac.gla.cvr.gluetools.core.requestQueue;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;

public class Request {

	private String modePath;
	private Command<?> command;
	private String[] commandWords;
	
	private String queueName = RequestQueue.DEFAULT_QUEUE_NAME;
	
	public Request(String modePath, Command<?> command) {
		super();
		this.modePath = modePath;
		this.command = command;
		this.commandWords = CommandUsage.cmdWordsForCmdClass(command.getClass());
	}

	public String getModePath() {
		return modePath;
	}
	
	public String[] getCommandWords() {
		return commandWords;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public String getQueueName() {
		return queueName;
	}

	public Command<?> getCommand() {
		return command;
	}

}
