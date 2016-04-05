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
			BytesDrainer outBytesDrainer = new BytesDrainer(processStdOut, outputBytes);
			Thread outBytesDrainerThread = new Thread(outBytesDrainer);
			BytesDrainer errBytesDrainer = new BytesDrainer(processStdErr, errorBytes);
			Thread errBytesDrainerThread = new Thread(errBytesDrainer);
			outBytesDrainerThread.start();
			errBytesDrainerThread.start();

			boolean inputExhausted = false;
			boolean processComplete = false;
			while(!processComplete) {
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
				GlueLogger.getGlueLogger().finest("waiting for complete");
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
		
		} finally {
			if(process != null && process.isAlive()) {
				try { process.destroyForcibly().waitFor(); } catch (InterruptedException e) {}
			}
		}
		return new ProcessResult(outputBytes.toByteArray(), errorBytes.toByteArray(), process.exitValue());

	}
	
	
	private static int drainBytes(InputStream fromStream, byte[] drainBuffer, OutputStream toStream) throws IOException {
//		int available = fromStream.available();
//		GlueLogger.getGlueLogger().finest("available: "+available);
//		int bytesRead = fromStream.read(drainBuffer, 0, Math.min(drainBuffer.length, available));
		int bytesRead = fromStream.read(drainBuffer, 0, drainBuffer.length);
		GlueLogger.getGlueLogger().finest("bytes read: "+bytesRead);
		if(bytesRead > 0) {
			toStream.write(drainBuffer, 0, bytesRead);
			GlueLogger.getGlueLogger().finest("flushing");
			toStream.flush();
		}
		GlueLogger.getGlueLogger().finest("drain complete");
		return bytesRead;
	}
	
	private static class BytesDrainer implements Runnable {
		private InputStream fromStream;
		private OutputStream toStream;
		private byte[] drainBuffer = new byte[DRAIN_BUFFER_SIZE];
		private IOException ioException;
		
		public BytesDrainer(InputStream fromStream, OutputStream toStream) {
			super();
			this.fromStream = fromStream;
			this.toStream = toStream;
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

	
}
