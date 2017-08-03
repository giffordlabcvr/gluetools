package uk.ac.gla.cvr.gluetools.core.webfiles;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManagerException.Code;

public class WebFilesManager implements Runnable {

	private static final int DAYS_UNTIL_EXPIRY = 1;
	private Path webFilesRootDir;
	public boolean keepRunning;

	public WebFilesManager(String webFilesRootDirString) {
		super();
		try {
			this.webFilesRootDir = Paths.get(webFilesRootDirString);
		} catch(InvalidPathException ipe) {
			throw new WebFilesManagerException(ipe, Code.INVALID_ROOT_PATH, webFilesRootDirString);
		}
		if(!Files.exists(webFilesRootDir) || !Files.isDirectory(webFilesRootDir)) {
			throw new WebFilesManagerException(Code.INVALID_ROOT_PATH, webFilesRootDir.toString());
		}
		this.keepRunning = true;
		
		Thread webFilesManagerThread = new Thread(this);
		
		webFilesManagerThread.start();
	}

	
	
	public synchronized boolean getKeepRunning() {
		return keepRunning;
	}

	public synchronized void setKeepRunning(boolean keepRunning) {
		this.keepRunning = keepRunning;
	}

	@Override
	public void run() {
		while(getKeepRunning()) {
			try {
				List<Path> subDirs = Files.list(webFilesRootDir)
				.filter(p -> Files.isDirectory(p))
				.collect(Collectors.toList());
				
				subDirs.forEach(subDir -> {
					if(isExpired(subDir)) {
						GlueLogger.getGlueLogger().finest("webFiles dir "+subDir.toString()+" has expired");
						delete(subDir);
						GlueLogger.getGlueLogger().finest("webFiles dir "+subDir.toString()+" deleted");
					} else {
						GlueLogger.getGlueLogger().finest("webFiles dir "+subDir.toString()+" is still live");
					}
				});
				Thread.sleep(200);
			} catch(Exception ioe) {
				GlueLogger.getGlueLogger().warning("Exception in main WebFilesManager loop: "+ioe.getLocalizedMessage());
			} 
			
		}
		
	}

	private void delete(Path subDir) {
		DeleteFileVisitor deleteFileVisitor = new DeleteFileVisitor();
		try {
			Files.walkFileTree(subDir, deleteFileVisitor);
			if(deleteFileVisitor.failedPath != null && deleteFileVisitor.failureException != null) {
				GlueLogger.getGlueLogger()
				.warning("Exception in delete, visiting path: "+
						deleteFileVisitor.failedPath.toString()+": "+
						deleteFileVisitor.failureException.getLocalizedMessage());
			}
		} catch (Exception e) {
			GlueLogger.getGlueLogger()
				.warning("Exception in delete method applied to: "+
						subDir.toString()+": "+
						e.getLocalizedMessage());
		}
		
	}

	private boolean hasExpired(FileTime fileTime) {
		Date currentDateTime = new Date(System.currentTimeMillis());
		long fileTimeMillis = fileTime.toMillis();
		Calendar expiryDateTime = Calendar.getInstance();
		expiryDateTime.setTimeInMillis(fileTimeMillis);
		expiryDateTime.add(Calendar.SECOND, 20);
        //expiryDateTime.add(Calendar.DATE, DAYS_UNTIL_EXPIRY);
		if(currentDateTime.compareTo(expiryDateTime.getTime()) < 0) {
			return false;
		} else {
			return true;
		}
	}
	
	private class ExpiryFileVisitor extends SimpleFileVisitor<Path> {
		int livePaths = 0;
		Exception failureException;
		Path failedFile;

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			FileTime lastModifiedTime = Files.getLastModifiedTime(file);
			if(!hasExpired(lastModifiedTime)) {
				livePaths++;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			FileTime lastModifiedTime = Files.getLastModifiedTime(dir);
			if(!hasExpired(lastModifiedTime)) {
				livePaths++;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException failureException) throws IOException {
			this.failureException = failureException;
			this.failedFile = file;
			return FileVisitResult.TERMINATE;
		}
	}

	private class DeleteFileVisitor extends SimpleFileVisitor<Path> {
		Exception failureException;
		Path failedPath;

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			try {
				Files.deleteIfExists(file);
			} catch(Exception e) {
				this.failureException = e;
				this.failedPath = file;
				return FileVisitResult.TERMINATE;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException failureException) throws IOException {
			this.failureException = failureException;
			this.failedPath = file;
			return FileVisitResult.TERMINATE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException ioe) throws IOException {
			if(ioe == null) {
				try {
					Files.deleteIfExists(dir);
				} catch(Exception e) {
					this.failureException = e;
					this.failedPath = dir;
					return FileVisitResult.TERMINATE;
				}
			}
			return FileVisitResult.CONTINUE;
		}

	}

	
	
	private boolean isExpired(Path subDir) {
		ExpiryFileVisitor expiryFileVisitor = new ExpiryFileVisitor();
		try {
			Files.walkFileTree(subDir, expiryFileVisitor);
			if(expiryFileVisitor.failedFile != null && expiryFileVisitor.failureException != null) {
				GlueLogger.getGlueLogger()
				.warning("Exception in isExpired, visiting file: "+
						expiryFileVisitor.failedFile.toString()+": "+
						expiryFileVisitor.failureException.getLocalizedMessage());
				return false;
			}
		} catch (Exception e) {
			GlueLogger.getGlueLogger()
				.warning("Exception in isExpired method applied to: "+
						subDir.toString()+": "+
						e.getLocalizedMessage());
			return false;
		}
		return expiryFileVisitor.livePaths == 0;
	}
	
	public String createSubDir() {
		String subDirUuid = UUID.randomUUID().toString();
		Path subDirPath = webFilesRootDir.resolve(Paths.get(subDirUuid));
		try {
			Files.createDirectory(subDirPath);
		} catch(Exception e) {
			throw new WebFilesManagerException(e, Code.SUBDIR_CREATION_FAILED, subDirPath.toString());
		}
		return subDirUuid;
	}
	
	
}
