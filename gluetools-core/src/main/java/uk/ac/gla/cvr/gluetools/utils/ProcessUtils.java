package uk.ac.gla.cvr.gluetools.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessUtils {

	private static final int PROCESS_WAIT_INTERVAL_MS = 5;
	private static final int DRAIN_BUFFER_SIZE = 8192;


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

	public static ProcessResult runProcess(byte[] inputByteArray, List<String> commandWords) {
		return runProcess(inputByteArray, commandWords.toArray(new String[]{}));
	}
	
	public static ProcessResult runProcess(byte[] inputByteArray, String... commandWords) {
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
		ByteArrayInputStream inputBytes = new ByteArrayInputStream(inputByteArray);
		ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
		ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
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
			int inputBytesRead = 0;
			while(!processComplete) {
				drainBytes(processStdOut, drainBuffer, outputBytes);
				drainBytes(processStdErr, drainBuffer, errorBytes);
				inputBytesRead += drainBytes(inputBytes, drainBuffer, processStdIn);
				if(inputBytesRead >= inputByteArray.length) { processStdIn.close(); }
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
		int numBytes = fromStream.available();
		if(numBytes > 0) {
			numBytes = Math.min(numBytes, drainBuffer.length);
			fromStream.read(drainBuffer, 0, numBytes);
			toStream.write(drainBuffer, 0, numBytes);
			toStream.flush();
			return numBytes;
		}
		return numBytes;
	}

	
}
