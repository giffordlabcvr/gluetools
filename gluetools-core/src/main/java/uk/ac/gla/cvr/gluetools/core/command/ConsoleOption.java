package uk.ac.gla.cvr.gluetools.core.command;

public enum ConsoleOption {

	LOAD_SAVE_PATH("load-save-path", "Absolute path for loading/saving file data", "/", null) {
		@Override
		public String getDefaultValue() {
			return System.getProperty("user.dir", "/");
		}
		
	},
	VERBOSE_ERROR("verbose-error", "If \"true\" full stack trace is shown for errors", "false", new String[]{"true", "false"}),
	ECHO_CMD_XML("echo-cmd-xml", "If \"true\" the XML form of each command is echoed", "false", new String[]{"true", "false"}),
	ECHO_CMD_JSON("echo-cmd-json", "If \"true\" the JSON form of each command is echoed", "false", new String[]{"true", "false"}),
	CMD_RESULT_FORMAT("cmd-result-format", "Configures the format for command results on the console", "text", new String[]{"text", "xml"});
	
	private final String name;
	private final String description;
	private final String defaultValue;
	private final String[] allowedValues;
	
	private ConsoleOption(String name, String description, String defaultValue, String[] allowedValues) {
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.allowedValues = allowedValues;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public String[] getAllowedValues() {
		return allowedValues;
	}
	
}
