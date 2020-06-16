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
package uk.ac.gla.cvr.gluetools.core.command.console;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.ConsoleOption;
import uk.ac.gla.cvr.gluetools.core.command.ParallelWorkerException;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OutputStreamCommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ResultOutputFormat;
import uk.ac.gla.cvr.gluetools.core.console.Console;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException.Code;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

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
	
	public static String getCanonicalPath(File file) {
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			throw new ConsoleException(e, Code.INVALID_PATH, file.toString(), e.getMessage());
		}
	}
	
	public void saveBytes(String fileString, byte[] bytes) {
		saveBytesToFile(fileStringToFile(fileString), bytes);
	}
	
	public static void saveBytesToFile(File file, byte[] bytes) {
		try(OutputStream fileOutputStream = openFile(file)) {
			IOUtils.write(bytes, fileOutputStream);
		} catch (IOException e) {
			throw new ConsoleException(e, Code.WRITE_ERROR, file, e.getMessage());
		}
	}

	public OutputStream openFile(String fileString) {
		return openFile(fileStringToFile(fileString));
	}
	
	public static OutputStream openFile(File file) {
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
		try {
			return new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			throw new ConsoleException(e, Code.FILE_NOT_FOUND, file);
		}
		
	}
	
	public void mkdirs(String fileString) {
		File dirFile = fileStringToFile(fileString);
		mkdirs(dirFile);
	}

	public void mkdirs(File dirFile) {
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
		List<String> result = Arrays.asList(dirFile.list(new FilenameFilter() {
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
		Collections.sort(result);
		return result;
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

	public void runBatchCommands(String batchFilePath, String batchContent, boolean noCmdEcho, boolean noCommentEcho, boolean noOutput) {
		if(this.isParallelWorker()) {
			throw new ParallelWorkerException(ParallelWorkerException.Code.PARALLEL_WORKER_ERROR, "Parallel worker may not us 'run file' command");
		}
		String[] lines = batchContent.split("\n");
		// in batch files, allow backslash to signify line continuation
		List<Object> linesList = Arrays.stream(lines).collect(Collectors.toList());
		List<Object> commandLinesList = new ArrayList<Object>();
		List<Integer> linesPerCommand = new ArrayList<Integer>();
		StringBuffer currentCmdBuf = null;
		int currentCmdLines = 0;
		for(Object obj: linesList) {
			String line = (String) obj;
			currentCmdLines++;
			if(currentCmdBuf == null) {
				currentCmdBuf = new StringBuffer();
			}
			if(line.endsWith("\\")) {
				currentCmdBuf.append(line.substring(0, line.length()-1));
			} else {
				currentCmdBuf.append(line);
				commandLinesList.add(currentCmdBuf.toString());
				linesPerCommand.add(currentCmdLines);
				currentCmdLines = 0;
				currentCmdBuf = null;
			}
		}
		if(currentCmdBuf != null) {
			commandLinesList.add(currentCmdBuf.toString());
			linesPerCommand.add(currentCmdLines);
		}
		console.runBatchCommands(batchFilePath, commandLinesList, linesPerCommand, noCmdEcho, noCommentEcho, noOutput);
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

	public void runScript(String filePath, String scriptContent) {
		if(this.isParallelWorker()) {
			throw new ParallelWorkerException(ParallelWorkerException.Code.PARALLEL_WORKER_ERROR, "Parallel worker may not us 'run script' command");
		}
		console.runScript(filePath, scriptContent);
	}

	public void saveCommandResult(Function<OutputStream, OutputStreamCommandResultRenderingContext> renderingContextGenerator, String filePath, CommandResult commandResult) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamCommandResultRenderingContext fileRenderingContext = renderingContextGenerator.apply(baos);
			commandResult.renderResult(fileRenderingContext);
			byte[] byteArray = baos.toByteArray();
			saveBytes(filePath, byteArray);
	}

	public boolean hasAuthorisation(String authorisationName) {
		return true;
	}

	@Override
	protected CommandContext createParallelWorkerInternal() {
		ConsoleCommandContext parallelWorker = new ConsoleCommandContext(getGluetoolsEngine(), null);
		this.optionToValue.forEach((option, value) -> {
			parallelWorker.optionToValue.put(option, value);
		}); 
		return parallelWorker;
	}
	

	
}
