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
package uk.ac.gla.cvr.gluetools.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.console.Lexer;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtilsException.Code;

public class ProcessUtils {

	private static final int PROCESS_WAIT_INTERVAL_MS = 5;
	private static final int DRAIN_BUFFER_SIZE = 65536;


	public static class ProcessResult {
		private byte[] outputBytes;
		private byte[] errorBytes;
		private int exitCode;
		
		private ProcessResult(byte[] outputBytes, byte[] errorBytes, int exitCode) {
			super();
			this.outputBytes = outputBytes;
			this.errorBytes = errorBytes;
			this.exitCode = exitCode;
		}

		public byte[] getOutputBytes() {
			return outputBytes;
		}

		public byte[] getErrorBytes() {
			return errorBytes;
		}

		public int getExitCode() {
			return exitCode;
		}
	}

	public static ProcessResult runProcess(InputStream inputStream, File workingDirectory, List<String> commandWords) {
		return runProcess(inputStream, workingDirectory, commandWords.toArray(new String[]{}));
	}
	
	public static ProcessResult runProcess(InputStream inputStream, File workingDirectory, String... commandWords) {
		if(commandWords.length == 0) {
			throw new ProcessUtilsException(ProcessUtilsException.Code.UNABLE_TO_START_PROCESS, 
					"No command words supplied");
		}
		if(commandWords[0] == null) {
			throw new ProcessUtilsException(ProcessUtilsException.Code.UNABLE_TO_START_PROCESS, 
					"First command word was null");
		}
		File executableFile = new File(commandWords[0]);
		if(!executableFile.canExecute()) {
			throw new ProcessUtilsException(ProcessUtilsException.Code.UNABLE_TO_START_PROCESS, 
					"Not an executable file: "+executableFile.getPath());
		}
		ProcessBuilder processBuilder = new ProcessBuilder(commandWords);
		if(workingDirectory != null) {
			processBuilder.directory(workingDirectory);
		}
		Process process = null;
		ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
		ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
		List<String> quotifiedCmdList = processBuilder.command().stream()
				.map(arg -> Lexer.quotifyIfNecessary(arg))
				.collect(Collectors.toList());
		GlueLogger.getGlueLogger().fine("Running external process: "+String.join(" ", quotifiedCmdList));
		byte[] drainBuffer = new byte[DRAIN_BUFFER_SIZE];
		Thread outBytesDrainerThread = null;
		Thread errBytesDrainerThread = null;
		boolean logStdErr = false;
		
		try {
			try {
				process = processBuilder.start();
			} catch(IOException ioe1) {
				throw new ProcessUtilsException(ioe1, 
						ProcessUtilsException.Code.UNABLE_TO_START_PROCESS_FOR_COMMAND, commandWords[0], 
						ioe1.getLocalizedMessage());
			}
			OutputStream processStdIn = process.getOutputStream();
			InputStream processStdOut = process.getInputStream();
			InputStream processStdErr = process.getErrorStream();
			BytesDrainer outBytesDrainer = new BytesDrainer(processStdOut, outputBytes, "stdout bytes drainer");
			outBytesDrainerThread = new Thread(outBytesDrainer);
			BytesDrainer errBytesDrainer = new BytesDrainer(processStdErr, errorBytes, "stderr bytes drainer");
			errBytesDrainerThread = new Thread(errBytesDrainer);
			outBytesDrainerThread.start();
			errBytesDrainerThread.start();

			boolean inputExhausted = false;
			boolean processComplete = false;
			while(!processComplete) {
				if(inputStream != null) {
					if(!inputExhausted) {
						try {
							int inBytes = drainBytes(inputStream, drainBuffer, processStdIn);
							if(inBytes < 0) { 
								processStdIn.close(); 
								inputExhausted = true;
							}
						} catch(IOException ioe2) {
							throw new ProcessUtilsException(ioe2, 
									ProcessUtilsException.Code.PROCESS_IO_STDIN_ERROR, commandWords[0], 
									ioe2.getLocalizedMessage());
						} 
					}
				}
				try {
					processComplete = process.waitFor(PROCESS_WAIT_INTERVAL_MS, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {}
				IOException outIoe = outBytesDrainer.getIoException();
				if(outIoe != null) {
					throw new ProcessUtilsException(outIoe, 
							ProcessUtilsException.Code.PROCESS_IO_STDOUT_ERROR, commandWords[0], 
							outIoe.getLocalizedMessage());
				}
				IOException errIoe = errBytesDrainer.getIoException();
				if(errIoe != null) {
					throw new ProcessUtilsException(errIoe, 
							ProcessUtilsException.Code.PROCESS_IO_STDERR_ERROR, commandWords[0], 
							errIoe.getLocalizedMessage());
				}
			}
			try {
				outBytesDrainerThread.join();
			} catch (InterruptedException e) {}
			try {
				errBytesDrainerThread.join();
			} catch (InterruptedException e) {}
		
		} catch(Throwable t) {
			logStdErr = true;
			throw t;
		} finally {
			if(logStdErr) {
				// give error bytes draining thread a chance to drain any further error output
				if(errBytesDrainerThread != null) {
					try {
						errBytesDrainerThread.join(1000);
					} catch (Throwable t) {}
				}
			}
			if(process != null && process.isAlive()) {
				try { process.destroyForcibly().waitFor(); } catch (InterruptedException e) {}
			}
			if(logStdErr) {
				if(errBytesDrainerThread != null) {
					try {
						// wait for error bytes draining thread to die
						errBytesDrainerThread.join(1000);
					} catch (Throwable t) {}
				}
				// log the contents of the standard error buffer.
				GlueLogger.getGlueLogger().severe("Process stderr contents:\n"+new String(errorBytes.toByteArray()));
			}
			
		}
		return new ProcessResult(outputBytes.toByteArray(), errorBytes.toByteArray(), process.exitValue());

	}
	
	
	private static int drainBytes(InputStream fromStream, byte[] drainBuffer, OutputStream toStream) throws IOException {
		int bytesRead = fromStream.read(drainBuffer, 0, drainBuffer.length);
		if(bytesRead > 0) {
			toStream.write(drainBuffer, 0, bytesRead);
			toStream.flush();
		}
		return bytesRead;
	}
	
	private static class BytesDrainer implements Runnable {
		private InputStream fromStream;
		private OutputStream toStream;
		private byte[] drainBuffer = new byte[DRAIN_BUFFER_SIZE];
		private IOException ioException;
		@SuppressWarnings("unused")
		private String name;
		
		public BytesDrainer(InputStream fromStream, OutputStream toStream, String name) {
			super();
			this.fromStream = fromStream;
			this.toStream = toStream;
			this.name = name;
		}

		public IOException getIoException() {
			return ioException;
		}

		@Override
		public void run() {
			while(true) {
				int bytesDrained;
				try {
					bytesDrained = drainBytes(fromStream, drainBuffer, toStream);
				} catch (IOException ioe) {
					this.ioException = ioe;
					break;
				}
				if(bytesDrained < 0) {
					break; // EOF
				}
			}			
		}
		
	}

	public static void cleanUpTempDir(File dataDirFile, File tempDir) {
		if(tempDir != null && tempDir.exists() && tempDir.isDirectory()) {
			boolean allFilesDeleted = true;
			for(File file : tempDir.listFiles()) {
				if(dataDirFile != null) {
					byte[] fileBytes = ConsoleCommandContext.loadBytesFromFile(file);
					File fileToSave = new File(dataDirFile, file.getName());
					ConsoleCommandContext.saveBytesToFile(fileToSave, fileBytes);
				}
				boolean fileDeleteResult = file.delete();
				if(!fileDeleteResult) {
					GlueLogger.getGlueLogger().warning("Failed to delete temporary file "+file.getAbsolutePath());
					allFilesDeleted = false;
					break;
				}
			}
			if(allFilesDeleted) {
				boolean dirDeleteResult = tempDir.delete();
				if(!dirDeleteResult) {
					GlueLogger.getGlueLogger().warning("Failed to delete temporary directory "+tempDir.getAbsolutePath());
				}
			}
		}
	}

	public static void checkExitCode(List<String> commandWords, ProcessResult processResult) {
		int exitCode = processResult.getExitCode();
		if(exitCode != 0) {
			String command = String.join(" ", commandWords);
			GlueLogger.getGlueLogger().severe("Process failure, the stdout was:");
			GlueLogger.getGlueLogger().severe(new String(processResult.getOutputBytes()));
			GlueLogger.getGlueLogger().severe("Process failure, the stderr was:");
			GlueLogger.getGlueLogger().severe(new String(processResult.getErrorBytes()));
			throw new ProcessUtilsException(Code.PROCESS_EXIT_CODE_ERROR, command, Integer.toString(exitCode));
		}
	}

	public static String normalisedFilePath(File file) {
		String normalizedPath = file.getAbsolutePath();
		if(System.getProperty("os.name").toLowerCase().contains("windows")) {
			normalizedPath = normalizedPath.replace('\\', '/');
		}
		return normalizedPath;
	}

	
}
