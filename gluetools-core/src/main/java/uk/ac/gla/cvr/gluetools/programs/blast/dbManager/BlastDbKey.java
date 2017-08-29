package uk.ac.gla.cvr.gluetools.programs.blast.dbManager;

import java.io.File;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastException;

public abstract class BlastDbKey<T extends BlastDB<?>> {

	public static String BLAST_DB_DIR_PROPERTY = "gluetools.core.programs.blast.db.dir";

	private String projectName;
	
	protected BlastDbKey(String projectName) {
		this.projectName = projectName;
	}

	protected String getProjectName() {
		return projectName;
	}
	
	
	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);	
	
	public abstract T createBlastDB();
	
	public final File getBlastDbDir(CommandContext cmdContext) {
		PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String blastDbStoragePath = propertiesConfiguration.getPropertyValue(BLAST_DB_DIR_PROPERTY);
		if(blastDbStoragePath == null) {
			throw new BlastException(BlastException.Code.BLAST_CONFIG_EXCEPTION, "BLAST DB directory not configured in config property "+BLAST_DB_DIR_PROPERTY);
		}
		if(blastDbStoragePath.contains(" ")) {
			throw new BlastException(BlastException.Code.INVALID_BLAST_DB_PATH, "Path configured in "+BLAST_DB_DIR_PROPERTY+" contains spaces, which will cause BLAST to fail, please reconfigure.");
		}
		File projectPath = new File(blastDbStoragePath, getProjectName());
		return getProjectRelativeBlastDbDir(projectPath);
	}
	
	protected abstract File getProjectRelativeBlastDbDir(File projectPath);


	
}
