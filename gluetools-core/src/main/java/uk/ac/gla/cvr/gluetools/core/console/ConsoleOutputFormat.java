package uk.ac.gla.cvr.gluetools.core.console;

public enum ConsoleOutputFormat {
	XML("xml"),
	JSON("json"),
	TEXT("text"),
	TAB("tab"),
	CSV("csv");

	private final String name;
	
	private ConsoleOutputFormat(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
}
