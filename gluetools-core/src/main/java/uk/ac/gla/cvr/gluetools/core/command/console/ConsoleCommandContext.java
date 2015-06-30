package uk.ac.gla.cvr.gluetools.core.command.console;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException.Code;

public class ConsoleCommandContext extends CommandContext {

	private File loadSavePath = new File(System.getProperty("user.dir", "/"));
	
	public ConsoleCommandContext(GluetoolsEngine gluetoolsEngine) {
		super(gluetoolsEngine);
	}

	private boolean finished = false;
	private boolean requireModeWrappable = false;

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public File getLoadSavePath() {
		return loadSavePath;
	}

	public void setLoadSavePath(File loadSavePath) {
		this.loadSavePath = loadSavePath;
	}

	public void updateLoadSavePath(String path) {
		File update = new File(path);
		if(update.isAbsolute()) {
			setLoadSavePath(update);
		} else {
			File concatenated = new File(getLoadSavePath(), path);
			try {
				setLoadSavePath(new File(concatenated.getCanonicalPath()));
			} catch (IOException e) {
				throw new ConsoleException(e, Code.INVALID_PATH, path, e.getMessage());
			}
		}
	}

	public byte[] loadBytes(String file) {
		File path = new File(file);
		if(!path.isAbsolute()) {
			path = new File(loadSavePath, file);
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

	

	

	
}
