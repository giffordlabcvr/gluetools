package uk.ac.gla.cvr.gluetools.core.command.console;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.ConsoleOption;
import uk.ac.gla.cvr.gluetools.core.console.Console;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException.Code;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

public class ConsoleCommandContext extends CommandContext {

	private Map<ConsoleOption, String> optionToValue = new LinkedHashMap<ConsoleOption, String>();
	
	private boolean finished = false;
	private boolean requireModeWrappable = false;
	private Console console;
	private Set<ConsoleOption> optionLines = new LinkedHashSet<ConsoleOption>();

	public ConsoleCommandContext(GluetoolsEngine gluetoolsEngine, Console console) {
		super(gluetoolsEngine, "the GLUE console");
		this.console = console;
	}

	public void unsetOptionValue(ConsoleOption option) {
		optionToValue.remove(option);
	}
	
	public void setOptionValue(ConsoleOption option, String value) {
		option.onSet(console, value);
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
	
	public void saveBytes(String fileString, byte[] bytes) {
		saveBytesToFile(fileStringToFile(fileString), bytes);
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

	public void mkdirs(String fileString) {
		File dirFile = fileStringToFile(fileString);
		dirFile.mkdirs();
		if(!dirFile.isDirectory()) {
			throw new ConsoleException(Code.MAKE_DIRECTORY_ERROR, dirFile);
		}
	}
	
	public List<String> listMembers(String pathString, boolean includeFiles, boolean includeDirectories, String prefix) {
		File dirFile = fileStringToFile(pathString);
		return listMembers(dirFile, includeFiles, includeDirectories, prefix);
	}

	public boolean isDirectory(String pathString) {
		File dirFile = fileStringToFile(pathString);
		return dirFile.isDirectory();
	}
	
	public boolean isFile(String pathString) {
		File file = fileStringToFile(pathString);
		return file.isFile();
	}

	public boolean delete(String pathString) {
		File file = fileStringToFile(pathString);
		return file.delete();
	}

	
	public List<String> listMembers(File dirFile, boolean includeFiles, boolean includeDirectories, String prefix) {
		if(!dirFile.isDirectory()) {
			throw new ConsoleException(Code.NOT_A_DIRECTORY, dirFile);
		}
		return Arrays.asList(dirFile.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				File theMember = new File(dir, name);
				if(prefix != null && !name.startsWith(prefix)) {
					return false;
				}
				if(includeFiles && theMember.isFile()) {
					return true;
				}
				if(includeDirectories && theMember.isDirectory()) {
					return true;
				}
				return false;
			}
		}));
	}

	public List<String> listMembers(boolean includeFiles, boolean includeDirectories, String prefix) {
		return listMembers(new File(getOptionValue(ConsoleOption.LOAD_SAVE_PATH)), includeFiles, includeDirectories, prefix);
	}
	
	
	public byte[] loadBytes(String fileString) {
		return loadBytesFromFile(fileStringToFile(fileString));
	}

	public File fileStringToFile(String fileString) {
		File path = new File(fileString);
		if(!path.isAbsolute()) {
			path = new File(getOptionValue(ConsoleOption.LOAD_SAVE_PATH), fileString);
		}
		try {
			path = path.getCanonicalFile();
		} catch (IOException e) {
			throw new ConsoleException(e, Code.INVALID_PATH, path, e.getMessage());
		}
		return path;
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

	public static InputStream inputStreamFromFile(File file) {
		if(!file.exists()) {
			throw new ConsoleException(Code.FILE_NOT_FOUND, file);
		}
		if(!file.isFile()) {
			throw new ConsoleException(Code.NOT_A_FILE, file);
		}
		if(!file.canRead()) {
			throw new ConsoleException(Code.FILE_NOT_READABLE, file);
		}
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new ConsoleException(Code.FILE_NOT_FOUND, file);
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

	public void runBatchCommands(String batchFilePath, String batchContent, boolean noEcho, boolean noOutput) {
		GlueLogger.getGlueLogger().finest("Started running GLUE batch "+batchFilePath);
		String[] lines = batchContent.split("\n");
		console.runBatchCommands(batchFilePath, Arrays.stream(lines).collect(Collectors.toList()), noEcho, noOutput);
		GlueLogger.getGlueLogger().finest("Finished running GLUE batch "+batchFilePath);
	}

	public void addOptionLine(ConsoleOption consoleOption) {
		optionLines.add(consoleOption);
	}

	public void removeOptionLine(ConsoleOption consoleOption) {
		optionLines.remove(consoleOption);
	}

	public Set<ConsoleOption> getOptionLines() {
		return optionLines;
	}

	public int getTerminalWidth() {
		return console.getTerminalWidth();
	}

	public int getTerminalHeight() {
		return console.getTerminalHeight();
	}

	

	

	
}
