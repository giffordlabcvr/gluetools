package uk.ac.gla.cvr.gluetools.core.command;

import java.io.File;
import java.util.logging.Level;

import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleOptionException;
import uk.ac.gla.cvr.gluetools.core.console.Console;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

public enum ConsoleOption {

	LOAD_SAVE_PATH("load-save-path", "Absolute path for loading/saving file data", "/", null) {
		@Override
		public String getDefaultValue() {
			return System.getProperty("user.dir", "/");
		}
		public void onSet(Console console, String newValue) {
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
	TABLE_RESULT_DOUBLE_PRECISION("table-result-float-precision", "Configures the decimal places precision for floating point numbers in table, tab and csv results", "full", new String[]{"full", "1", "2", "3", "4"}),
	SAVE_COMMAND_HISTORY("save-cmd-history", "Configures when command history is saved", "after_every_cmd", new String[]{"after_every_cmd", "at_end_of_session", "never"}),
	MAX_COMMAND_HISTORY_SIZE("max-cmd-history-size", "Maximum number of commands in command history", "100", null) {
		public void onSet(Console console, String newValue) {
			boolean bad = false;
			Integer maxCmdHistorySize = null;
			try {
				maxCmdHistorySize = Integer.parseInt(newValue);
				if(maxCmdHistorySize <= 0) {
					bad = true;
				} else {
					console.setMaxCmdHistorySize(maxCmdHistorySize);
				}
			} catch(NumberFormatException nfe) {
				bad = true;
			}
			if(bad) {
				throw new ConsoleOptionException(ConsoleOptionException.Code.INVALID_OPTION_VALUE, "max-cmd-history", newValue, "Not a positive integer");
			}
		}
	},
	COMPLETER_OPTIONS_DISPLAY("completer-options-display", "Configures whether long, short, or both forms of options are displayed during command completion", "short_only", new String[]{"short_only", "long_only", "both"}),
	LOG_LEVEL("log-level", "Configures the level of detail in the GLUE logger category", null, GlueLogger.ALL_LOG_LEVELS) {
		@Override
		public String getDefaultValue() {
			return GlueLogger.getGlueLogger().getLevel().getName();
		}

		@Override
		public void onSet(Console console, String newValue) {
			super.onSet(console, newValue);
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
	
	public void onSet(Console console, String newValue) {
		
	}
	
}
