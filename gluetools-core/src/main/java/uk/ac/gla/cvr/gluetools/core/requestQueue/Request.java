package uk.ac.gla.cvr.gluetools.core.requestQueue;

import java.util.List;

import org.w3c.dom.Document;

public class Request {

	private String modePath;
	private String[] commandWords;
	private Document cmdXmlDoc;
	
	private String queueName;
	
	public Request(String modePath, String[] commandWords, Document cmdXmlDoc) {
		super();
		this.modePath = modePath;
		this.commandWords = commandWords;
		this.cmdXmlDoc = cmdXmlDoc;
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

	public Document getCmdXmlDoc() {
		return cmdXmlDoc;
	}

}
