package uk.ac.gla.cvr.gluetools.programs.blast.dbManager;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;

public abstract class BlastDB {
	
	public static String 
		BLAST_DB_DIR_PROPERTY = "gluetools.core.programs.blast.db.dir";

	private String project;
	private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	protected BlastDB(String project) {
		super();
		this.project = project;
	}

	public String getProjectName() {
		return project;
	}
	
	public final File getBlastDbDir(CommandContext cmdContext) {
		PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String blastDbStoragePath = propertiesConfiguration.getPropertyValue(BLAST_DB_DIR_PROPERTY);
		File projectPath = new File(blastDbStoragePath, getProjectName());
		return getProjectRelativeBlastDbDir(projectPath);
	}
	
	protected abstract File getProjectRelativeBlastDbDir(File projectPath);

	public WriteLock writeLock() {
		return readWriteLock.writeLock();
	}
	public ReadLock readLock() {
		return readWriteLock.readLock();
	}
	
	public abstract String getTitle();

	public abstract long getLastUpdateTime(CommandContext cmdContext);
	
	public abstract InputStream getFastaContentInputStream(CommandContext cmdContext);

}
