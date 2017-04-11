package uk.ac.gla.cvr.gluetools.core.command.result;

public enum ResultOutputFormat {
	XML("xml"),
	JSON("json"),
	TEXT("text"),
	TAB("tab"),
	CSV("csv");

	private final String name;
	
	private ResultOutputFormat(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
}
