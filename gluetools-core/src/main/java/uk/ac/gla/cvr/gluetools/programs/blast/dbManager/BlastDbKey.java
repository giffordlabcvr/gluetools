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
