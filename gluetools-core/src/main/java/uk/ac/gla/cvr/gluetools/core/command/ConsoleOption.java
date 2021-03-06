/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
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
	TABLE_RESULT_DOUBLE_PRECISION("table-result-float-precision", "Configures the decimal places precision for floating point numbers in console table results", "full", new String[]{"full", "1", "2", "3", "4"}),
	TABLE_TRUNCATION_LIMIT("table-truncation-limit", "Configures the maximum cell string length in interactive table results", "50", new String[]{"10", "25", "50", "100", "250", "500", "1000"}),
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
	MAX_COMMAND_HISTORY_LINE_LENGTH("max-cmd-history-line-length", "Maximum length of a line stored in the command history in characters", "2000", null) {
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
		
	},
	CMD_OUTPUT_FILE_FORMAT("cmd-output-file-format", "Configures the format for command results saved to files", "xml", new String[]{"xml", "json", "tab", "csv"}),
	NEXT_CMD_OUTPUT_FILE("next-cmd-output-file", "The next command's result will be saved to this file, then this option will be unset", null, null) {
		@Override
		public List<CompletionSuggestion> instantiateValue(
				ConsoleCommandContext cmdContext,
				@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			return AdvancedCmdCompleter.completePath(cmdContext, prefix, false);
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

	public List<CompletionSuggestion> instantiateValue(
			ConsoleCommandContext cmdContext,
			@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
			String prefix) {
		String[] allowedValues = getAllowedValues();
		if(allowedValues != null) {
			return Arrays.asList(allowedValues).stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
		} 
		return null;
	}
	
}
