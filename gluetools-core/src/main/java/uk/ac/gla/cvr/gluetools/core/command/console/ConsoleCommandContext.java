package uk.ac.gla.cvr.gluetools.core.command.console;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.ConsoleOption;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException.Code;

public class ConsoleCommandContext extends CommandContext {

	private Map<ConsoleOption, String> optionToValue = new LinkedHashMap<ConsoleOption, String>();
	
	public ConsoleCommandContext(GluetoolsEngine gluetoolsEngine) {
		super(gluetoolsEngine, "the GLUE console");
	}

	private boolean finished = false;
	private boolean requireModeWrappable = false;

	public void unsetOptionValue(ConsoleOption option) {
		optionToValue.remove(option);
	}
	
	public void setOptionValue(ConsoleOption option, String value) {
		optionToValue.put(option, value);
	}
	
	public String getOptionValue(ConsoleOption option) {
		return optionToValue.getOrDefault(option, option.getDefaultValue());
	}

	public String getConfiguredOptionValue(ConsoleOption option) {
		return optionToValue.get(option);
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public File getLoadSavePath() {
		return new File(getOptionValue(ConsoleOption.LOAD_SAVE_PATH));
	}

	public void updateLoadSavePath(String path) {
		File update = new File(path);
		if(!update.isAbsolute()) {
			update = new File(getLoadSavePath(), path);
		}
		try {
			setOptionValue(ConsoleOption.LOAD_SAVE_PATH, update.getCanonicalPath());
		} catch (IOException e) {
			throw new ConsoleException(e, Code.INVALID_PATH, path, e.getMessage());
		}
	}
	
	public void saveBytes(String file, byte[] bytes) {
		File path = new File(file);
		if(!path.isAbsolute()) {
			path = new File(getOptionValue(ConsoleOption.LOAD_SAVE_PATH), file);
		}
		try {
			path = path.getCanonicalFile();
		} catch (IOException e) {
			throw new ConsoleException(e, Code.INVALID_PATH, path, e.getMessage());
		}
		saveBytesToFile(path, bytes);
	}
	
	public static void saveBytesToFile(File file, byte[] bytes) {
		if(file.exists()) {
			if(!file.canWrite()) {
				throw new ConsoleException(Code.FILE_NOT_WRITEABLE, file);
			}
		} else {
			try {
				file.createNewFile();
			} catch (IOException e) {
				throw new ConsoleException(e, Code.FILE_CREATION_ERROR, file, e.getMessage());
			} 
		}
		try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
			IOUtils.write(bytes, fileOutputStream);
		} catch (IOException e) {
			throw new ConsoleException(e, Code.WRITE_ERROR, file, e.getMessage());
		}
	}

	

	public byte[] loadBytes(String file) {
		File path = new File(file);
		if(!path.isAbsolute()) {
			path = new File(getOptionValue(ConsoleOption.LOAD_SAVE_PATH), file);
		}
		try {
			path = path.getCanonicalFile();
		} catch (IOException e) {
			throw new ConsoleException(e, Code.INVALID_PATH, path, e.getMessage());
		}
		return loadBytesFromFile(path);
	}

	public static byte[] loadBytesFromFile(File file) {
		if(!file.exists()) {
			throw new ConsoleException(Code.FILE_NOT_FOUND, file);
		}
		if(!file.isFile()) {
			throw new ConsoleException(Code.NOT_A_FILE, file);
		}
		if(!file.canRead()) {
			throw new ConsoleException(Code.FILE_NOT_READABLE, file);
		}
		try(FileInputStream fileInputStream = new FileInputStream(file)) {
			return IOUtils.toByteArray(fileInputStream);
		} catch (IOException e) {
			throw new ConsoleException(e, Code.READ_ERROR, file, e.getMessage());
		}
	}

	public boolean isRequireModeWrappable() {
		return requireModeWrappable;
	}

	public void setRequireModeWrappable(boolean requireModeWrappable) {
		this.requireModeWrappable = requireModeWrappable;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void checkCommmandIsExecutable(Class<? extends Command> cmdClass) {
		super.checkCommmandIsExecutable(cmdClass);
		if(CommandUsage.hasMetaTagForCmdClass(cmdClass, CmdMeta.webApiOnly)) {
			throw new CommandException(CommandException.Code.NOT_EXECUTABLE_IN_CONTEXT, 
					String.join(" ", CommandUsage.cmdWordsForCmdClass(cmdClass)), 
							getDescription());
		}
		if(requireModeWrappable && CommandUsage.hasMetaTagForCmdClass(cmdClass, CmdMeta.nonModeWrappable)) {
			throw new ConsoleException(Code.COMMAND_NOT_WRAPPABLE, 
					String.join(" ", CommandUsage.cmdWordsForCmdClass(cmdClass)), getModePath());
		}


	}

	

	

	
}
