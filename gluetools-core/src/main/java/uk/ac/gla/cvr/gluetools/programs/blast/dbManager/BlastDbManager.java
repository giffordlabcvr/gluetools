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
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.MultiReferenceBlastDB.MultiReferenceBlastDbKey;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.SingleReferenceBlastDB.SingleReferenceBlastDbKey;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.TemporaryMultiSeqBlastDB.TemporaryMultiSeqBlastDbKey;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.TemporarySingleSeqBlastDB.TemporarySingleSeqBlastDbKey;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;

public class BlastDbManager {

	private static BlastDbManager instance;
	private LinkedHashMap<BlastDbKey<?>, BlastDB<?>> blastDbsMap = 
			new LinkedHashMap<BlastDbKey<?>, BlastDB<?>>();
	
	public static String 
		MAKEBLASTDB_EXECUTABLE_PROPERTY = "gluetools.core.programs.blast.makeblastdb.executable";

	public static String 
		BLAST_DB_PREFIX = "blastDB";
	
	public synchronized static BlastDbManager getInstance() {
		if(instance == null) {
			instance = new BlastDbManager();
		}
		return instance;
	}
	
	private BlastDbManager() {}
	
	public SingleReferenceBlastDB ensureSingleReferenceDB(CommandContext cmdContext, String refName) {
		SingleReferenceBlastDbKey blastDbKey = new SingleReferenceBlastDbKey(getProjectName(cmdContext), refName);
		return (SingleReferenceBlastDB) ensureBlastDB(cmdContext, blastDbKey);
	}

	public MultiReferenceBlastDB ensureMultiReferenceDB(CommandContext cmdContext, String name, Set<String> refNames) {
		MultiReferenceBlastDbKey blastDbKey = new MultiReferenceBlastDbKey(getProjectName(cmdContext), name, refNames);
		return (MultiReferenceBlastDB) ensureBlastDB(cmdContext, blastDbKey);
	}

	
	public TemporarySingleSeqBlastDB createTempSingleSeqBlastDB(CommandContext cmdContext, String uuid, String refFastaID, String refNTs) {
		TemporarySingleSeqBlastDbKey blastDbKey = new TemporarySingleSeqBlastDbKey(getProjectName(cmdContext), uuid, refFastaID, refNTs);
		return (TemporarySingleSeqBlastDB) ensureBlastDB(cmdContext, blastDbKey);
	}

	public TemporaryMultiSeqBlastDB createTempMultiSeqBlastDB(CommandContext cmdContext, String uuid, Map<String, DNASequence> sequences) {
		TemporaryMultiSeqBlastDbKey blastDbKey = new TemporaryMultiSeqBlastDbKey(getProjectName(cmdContext), uuid, sequences);
		return (TemporaryMultiSeqBlastDB) ensureBlastDB(cmdContext, blastDbKey);
	}

	public void removeSingleRefBlastDB(CommandContext cmdContext, String refName) {
		removeDb(cmdContext, new SingleReferenceBlastDbKey(getProjectName(cmdContext), refName));
	}

	public void removeMultiRefBlastDB(CommandContext cmdContext, String dbName) {
		removeDb(cmdContext, new MultiReferenceBlastDbKey(getProjectName(cmdContext), dbName, null));
	}

	
	public void removeTempSingleSeqBlastDB(CommandContext cmdContext, String uuid) {
		removeDb(cmdContext, new TemporarySingleSeqBlastDbKey(getProjectName(cmdContext), uuid, "", ""));
	}

	
	public void removeTempMultiSeqBlastDB(CommandContext cmdContext, String uuid) {
		removeDb(cmdContext, new TemporaryMultiSeqBlastDbKey(getProjectName(cmdContext), uuid, null));
	}

	private void removeDb(CommandContext cmdContext,
			BlastDbKey<?> blastDbKey) {
		synchronized(blastDbsMap) {
			blastDbsMap.remove(blastDbKey);
			File blastDbDir = blastDbKey.getBlastDbDir(cmdContext);
			deleteDir(blastDbDir);
			
		}
	}

	private void deleteDir(File dbDir) {
		if(dbDir != null && dbDir.exists() && dbDir.isDirectory()) {
			boolean allFilesDeleted = true;
			for(File file : dbDir.listFiles()) {
				boolean fileDeleteResult = file.delete();
				if(!fileDeleteResult) {
					GlueLogger.getGlueLogger().warning("Failed to delete BLAST db file "+file.getAbsolutePath());
					allFilesDeleted = false;
					break;
				}
			}
			if(allFilesDeleted) {
				boolean dirDeleteResult = dbDir.delete();
				if(!dirDeleteResult) {
					GlueLogger.getGlueLogger().warning("Failed to delete BLAST db directory "+dbDir.getAbsolutePath());
				}
			}
		}
	}

	
	
	private String getProjectName(CommandContext cmdContext) {
		return ((InsideProjectMode) cmdContext.peekCommandMode()).getProject().getName();
	}

	@SuppressWarnings("unchecked")
	private <X extends BlastDB<?>> BlastDB<X> ensureBlastDB(CommandContext cmdContext, BlastDbKey<X> blastDbKey) {
		BlastDB<X> blastDB;
		synchronized(blastDbsMap) {
			blastDB = (BlastDB<X>) blastDbsMap.get(blastDbKey);
			if(blastDB == null) {
				blastDB = (BlastDB<X>) blastDbKey.createBlastDB();
				blastDbsMap.put(blastDbKey, blastDB);
			}
			File blastDbDir = blastDbKey.getBlastDbDir(cmdContext);
			blastDbDir.mkdirs();
			boolean cleanupRequired = false;
			try {
				blastDB.writeLock().lock();
				long lastUpdateTime = blastDB.getLastUpdateTime(cmdContext);
				File nhrFile = new File(blastDbDir, BLAST_DB_PREFIX+".nhr");
				if(nhrFile.exists() && nhrFile.lastModified() > lastUpdateTime) {
					return blastDB;
				}
				cleanupRequired = true;
				PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
				String makeBlastDbExecutable = propertiesConfiguration.getPropertyValue(MAKEBLASTDB_EXECUTABLE_PROPERTY);
				String title = blastDB.getTitle();
				ProcessResult processResult = ProcessUtils.runProcess(blastDB.getFastaContentInputStream(cmdContext), 
						null,
						makeBlastDbExecutable, 
						"-in", "-", // sequence data will be piped into the input stream.
						// Note: parse_seqids is unnecessary at present
						// "-parse_seqids", // allows subsequence BLASTN calls to filter target set by sequence IDs 
						"-dbtype", "nucl", // nucleotides
						"-title", title, // title
						"-out", new File(blastDbDir, BLAST_DB_PREFIX).getAbsolutePath() // output DB path
						);
				int exitCode = processResult.getExitCode();
				if(exitCode != 0) {
					GlueLogger.getGlueLogger().info(new String(processResult.getOutputBytes()));
					throw new BlastDbManagerException(BlastDbManagerException.Code.MAKE_BLAST_DB_FAILED, 
							blastDbDir, title, exitCode, new String(processResult.getErrorBytes()));
				} else {
					cleanupRequired = false;
				}
				return blastDB;
			} finally {
				if(cleanupRequired) { cleanup(blastDbDir); }
				blastDB.writeLock().unlock();
			}
		}

	}
	
	private void cleanup(File blastDbDir) {
		File[] dbFiles = blastDbDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return true;
			}
		});
		if(dbFiles != null) {
			for(File dbFile: dbFiles) {
				try {
					Files.delete(dbFile.toPath());
				} catch(Exception e) {
					GlueLogger.getGlueLogger().warning("Unable to clean up BLAST DB file: "+dbFile.getAbsolutePath());
				}
			}
		}
	}

	public void invalidateDB(String dbName) {
		// TODO Auto-generated method stub
		
	}



	
}
