package uk.ac.gla.cvr.gluetools.core.command;

import java.io.File;
import java.util.logging.Level;

import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleOptionException;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

public enum ConsoleOption {

	LOAD_SAVE_PATH("load-save-path", "Absolute path for loading/saving file data", "/", null) {
		@Override
		public String getDefaultValue() {
			return System.getProperty("user.dir", "/");
		}
		public void onSet(String newValue) {
			File newPath = new File(newValue);
			if(!newPath.isDirectory()) {
				throw new ConsoleOptionException(ConsoleOptionException.Code.INVALID_OPTION_VALUE, "load-save-path", newValue, "No such directory");
			}
		}
		
	},
	VERBOSE_ERROR("verbose-error", "If \"true\" full stack trace is shown for errors", "false", new String[]{"true", "false"}),
	ECHO_CMD_XML("echo-cmd-xml", "If \"true\" the XML form of each command is echoed", "false", new String[]{"true", "false"}),
	ECHO_CMD_JSON("echo-cmd-json", "If \"true\" the JSON form of each command is echoed", "false", new String[]{"true", "false"}),
	INTERACTIVE_TABLES("interactive-tables", "If \"true\" table results may be browsed interactively", "true", new String[]{"true", "false"}),
	CMD_RESULT_FORMAT("cmd-result-format", "Configures the format for command results on the console", "text", new String[]{"text", "xml", "json", "tab", "csv"}),
	LOG_LEVEL("log-level", "Configures the level of detail in the GLUE logger category", null, new String[]{
			Level.OFF.getName(), Level.SEVERE.getName(), Level.WARNING.getName(), Level.INFO.getName(),
			Level.CONFIG.getName(), Level.FINE.getName(), Level.FINER.getName(), Level.FINEST.getName(),
			Level.ALL.getName()
	}) {
		@Override
		public String getDefaultValue() {
			return GlueLogger.getGlueLogger().getLevel().getName();
		}

		@Override
		public void onSet(String newValue) {
			super.onSet(newValue);
			GlueLogger.getGlueLogger().setLevel(Level.parse(newValue));
		}
		
	};
	
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
	
	public void onSet(String newValue) {
		
	}
	
}
