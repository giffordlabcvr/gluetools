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
package uk.ac.gla.cvr.gluetools.core.webfiles;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.webfiles.WebFilesManagerException.Code;

public class WebFilesManager implements Runnable {

	private static final int HOURS_UNTIL_EXPIRY = 2;
	private Path webFilesRootDir;
	private Path webPagesDir;
	private Path downloadsDir;
	public boolean keepRunning;

	public enum WebFileType {
		WEB_PAGE("webPages"),
		DOWNLOAD("downloads");
		
		private String dirName;

		private WebFileType(String dirName) {
			this.dirName = dirName;
		}
		
		public String dirName() {
			return dirName;
		};
	}
	
	public WebFilesManager(String webFilesRootDirString) {
		super();
		try {
			this.webFilesRootDir = Paths.get(webFilesRootDirString);
			this.webPagesDir = webFilesRootDir.resolve(WebFileType.WEB_PAGE.dirName);
			this.downloadsDir = webFilesRootDir.resolve(WebFileType.DOWNLOAD.dirName);
		} catch(InvalidPathException ipe) {
			throw new WebFilesManagerException(ipe, Code.INVALID_ROOT_PATH, webFilesRootDirString);
		}
		if(!Files.exists(webFilesRootDir) || !Files.isDirectory(webFilesRootDir)) {
			throw new WebFilesManagerException(Code.INVALID_ROOT_PATH, webFilesRootDir.toString());
		}
		try {
			if(!Files.exists(downloadsDir)) {
				Files.createDirectory(downloadsDir);
			}
		} catch(IOException ioe) {
			throw new WebFilesManagerException(ioe, Code.INITIALISATION_FAILED, "Unable to create downloads directory: "+ioe.getLocalizedMessage());
		}
		try {
			if(!Files.exists(webPagesDir)) {
				Files.createDirectory(webPagesDir);
			}
		} catch(IOException ioe) {
			throw new WebFilesManagerException(ioe, Code.INITIALISATION_FAILED, "Unable to create webPages directory: "+ioe.getLocalizedMessage());
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
			try(Stream<Path> subDirsStream = Stream.concat(Files.list(webPagesDir), Files.list(downloadsDir))) {
				subDirsStream
				.filter(p -> Files.isDirectory(p))
				.forEach(subDir -> {
					if(isExpired(subDir)) {
						//GlueLogger.getGlueLogger().finest("webFiles dir "+subDir.toString()+" has expired");
						delete(subDir);
						//GlueLogger.getGlueLogger().finest("webFiles dir "+subDir.toString()+" deleted");
					} else {
						//GlueLogger.getGlueLogger().finest("webFiles dir "+subDir.toString()+" is still live");
					}
				});
				Thread.sleep(10000);
			} catch(Exception ioe) {
				// had to take this logging line out as it was exacerbating disk-full errors.
				//GlueLogger.getGlueLogger().warning("Exception in main WebFilesManager loop: "+ioe.getLocalizedMessage());
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
		expiryDateTime.add(Calendar.HOUR, HOURS_UNTIL_EXPIRY);
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
	
	public String createSubDir(WebFileType webFileType) {
		String subDirUuid = UUID.randomUUID().toString();
		Path subDirPath = getTypeDir(webFileType).resolve(Paths.get(subDirUuid));
		try {
			Files.createDirectory(subDirPath);
		} catch(Exception e) {
			throw new WebFilesManagerException(e, Code.SUBDIR_CREATION_FAILED, subDirPath.toString());
		}
		return subDirUuid;
	}
	
	public void createWebFileResource(WebFileType webFileType, String subDirUuid, String fileName) {
		Path subDirPath = getTypeDir(webFileType).resolve(Paths.get(subDirUuid));
		Path filePath = subDirPath.resolve(Paths.get(fileName));
		try {
			Files.createFile(filePath);
		} catch(Exception e) {
			throw new WebFilesManagerException(e, Code.FILE_CREATION_FAILED, filePath.toString());
		}
	}

	public OutputStream appendToWebFileResource(WebFileType webFileType, String subDirUuid, String fileName) {
		Path subDirPath = getTypeDir(webFileType).resolve(Paths.get(subDirUuid));
		Path filePath = subDirPath.resolve(Paths.get(fileName));
		try {
			return Files.newOutputStream(filePath, StandardOpenOption.APPEND);
		} catch(Exception e) {
			throw new WebFilesManagerException(e, Code.FILE_APPEND_FAILED, filePath.toString());
		}
	}

	public String getSizeString(WebFileType webFileType, String subDirUuid, String fileName) {
		long size = getSize(webFileType, subDirUuid, fileName);
		return humanReadableByteCount(size, true);
	}



	public long getSize(WebFileType webFileType, String subDirUuid, String fileName) {
		Path subDirPath = getTypeDir(webFileType).resolve(Paths.get(subDirUuid));
		Path filePath = subDirPath.resolve(Paths.get(fileName));
		long size;
		try {
			size = Files.size(filePath);
		} catch(Exception e) {
			throw new WebFilesManagerException(e, Code.FILE_SIZE_FAILED, filePath.toString());
		}
		return size;
	}

	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public Path getTypeDir(WebFileType webFileType) {
		if(webFileType == WebFileType.DOWNLOAD) {
			return downloadsDir;
		}
		if(webFileType == WebFileType.WEB_PAGE) {
			return webPagesDir;
		}
		return null;
	}

	public Path getWebFilesRootDir() {
		return webFilesRootDir;
	}

}
