package uk.ac.gla.cvr.gluetools.programs.blast.dbManager;

public abstract class BlastDbKey<T extends BlastDB> {

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);	
	
	public abstract T createBlastDB();
	
}
