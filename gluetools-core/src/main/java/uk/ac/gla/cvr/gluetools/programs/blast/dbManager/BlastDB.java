package uk.ac.gla.cvr.gluetools.programs.blast.dbManager;

import java.io.InputStream;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;

public abstract class BlastDB<X extends BlastDB<?>> {
	
	private BlastDbKey<X> key;
	private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	protected BlastDB(BlastDbKey<X> key) {
		super();
		this.key = key;
	}

	public BlastDbKey<X> getKey() {
		return key;
	}
	
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
