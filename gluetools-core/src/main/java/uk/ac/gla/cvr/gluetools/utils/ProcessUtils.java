package uk.ac.gla.cvr.gluetools.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.console.Lexer;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

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

	public static ProcessResult runProcess(InputStream inputStream, List<String> commandWords) {
		return runProcess(inputStream, commandWords.toArray(new String[]{}));
	}
	
	public static ProcessResult runProcess(InputStream inputStream, String... commandWords) {
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
		Process process = null;
		ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
		ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
		List<String> quotifiedCmdList = processBuilder.command().stream()
				.map(arg -> Lexer.quotifyIfNecessary(arg))
				.collect(Collectors.toList());
		GlueLogger.getGlueLogger().fine("Running external process: "+String.join(" ", quotifiedCmdList));
		byte[] drainBuffer = new byte[DRAIN_BUFFER_SIZE];
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
			boolean processComplete = false;
			while(!processComplete) {
				GlueLogger.getGlueLogger().finest("draining process stdout");
				drainBytes(processStdOut, drainBuffer, outputBytes);
				GlueLogger.getGlueLogger().finest("draining process stderr");
				drainBytes(processStdErr, drainBuffer, errorBytes);
				GlueLogger.getGlueLogger().finest("draining process stdin");
				int inBytes = drainBytes(inputStream, drainBuffer, processStdIn);
				if(inBytes < 0) { 
					processStdIn.close(); 
				}
				GlueLogger.getGlueLogger().finest("waiting for complete");
				try {
					processComplete = process.waitFor(PROCESS_WAIT_INTERVAL_MS, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {}
			}
			while(drainBytes(processStdOut, drainBuffer, outputBytes) > 0){};
			while(drainBytes(processStdErr, drainBuffer, errorBytes) > 0){};
		} catch(IOException ioe2) {
			throw new ProcessUtilsException(ioe2, 
					ProcessUtilsException.Code.PROCESS_IO_ERROR, commandWords[0], 
					ioe2.getLocalizedMessage());
		} finally {
			if(process != null && process.isAlive()) {
				try { process.destroyForcibly().waitFor(); } catch (InterruptedException e) {}
			}
		}
		return new ProcessResult(outputBytes.toByteArray(), errorBytes.toByteArray(), process.exitValue());

	}
	
	
	private static int drainBytes(InputStream fromStream, byte[] drainBuffer, OutputStream toStream) throws IOException {
		int available = fromStream.available();
		GlueLogger.getGlueLogger().finest("available: "+available);
		int bytesRead = fromStream.read(drainBuffer, 0, Math.min(drainBuffer.length, available));
		GlueLogger.getGlueLogger().finest("bytes read: "+bytesRead);
		if(bytesRead > 0) {
			toStream.write(drainBuffer, 0, bytesRead);
			GlueLogger.getGlueLogger().finest("flushing");
			toStream.flush();
		}
		GlueLogger.getGlueLogger().finest("drain complete");
		return bytesRead;
	}

	
}
