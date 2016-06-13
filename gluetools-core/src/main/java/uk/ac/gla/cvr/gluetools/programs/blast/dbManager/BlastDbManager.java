package uk.ac.gla.cvr.gluetools.programs.blast.dbManager;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.SingleReferenceBlastDB.SingleReferenceBlastDbKey;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.TemporaryMultiSeqBlastDB.TemporaryMultiSeqBlastDbKey;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.TemporarySingleSeqBlastDB.TemporarySingleSeqBlastDbKey;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;

public class BlastDbManager {

	private static BlastDbManager instance;
	private LinkedHashMap<BlastDbKey<?>, BlastDB> blastDbsMap = 
			new LinkedHashMap<BlastDbKey<?>, BlastDB>();
	
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

	
	public TemporarySingleSeqBlastDB createTempSingleSeqBlastDB(CommandContext cmdContext, String uuid, String refFastaID, String refNTs) {
		TemporarySingleSeqBlastDbKey blastDbKey = new TemporarySingleSeqBlastDbKey(getProjectName(cmdContext), uuid, refFastaID, refNTs);
		return (TemporarySingleSeqBlastDB) ensureBlastDB(cmdContext, blastDbKey);
	}

	public TemporaryMultiSeqBlastDB createTempMultiSeqBlastDB(CommandContext cmdContext, String uuid, Map<String, DNASequence> sequences) {
		TemporaryMultiSeqBlastDbKey blastDbKey = new TemporaryMultiSeqBlastDbKey(getProjectName(cmdContext), uuid, sequences);
		return (TemporaryMultiSeqBlastDB) ensureBlastDB(cmdContext, blastDbKey);
	}

	
	public boolean removeTempSingleSeqBlastDB(CommandContext cmdContext, String uuid) {
		TemporarySingleSeqBlastDbKey blastDbKey = new TemporarySingleSeqBlastDbKey(getProjectName(cmdContext), uuid, "", "");
		BlastDB blastDB;
		synchronized(blastDbsMap) {
			blastDB = blastDbsMap.remove(blastDbKey);
			if(blastDB == null) {
				return false;
			}
		}
		File blastDbDir = getBlastDbDir(cmdContext, blastDB);
		return blastDbDir.delete();
	}

	
	public boolean removeTempMultiSeqBlastDB(CommandContext cmdContext, String uuid) {
		TemporaryMultiSeqBlastDbKey blastDbKey = new TemporaryMultiSeqBlastDbKey(getProjectName(cmdContext), uuid, null);
		BlastDB blastDB;
		synchronized(blastDbsMap) {
			blastDB = blastDbsMap.remove(blastDbKey);
			if(blastDB == null) {
				return false;
			}
		}
		File blastDbDir = getBlastDbDir(cmdContext, blastDB);
		return blastDbDir.delete();
	}

	
	private String getProjectName(CommandContext cmdContext) {
		return ((ProjectMode) cmdContext.peekCommandMode()).getProject().getName();
	}

	private BlastDB ensureBlastDB(CommandContext cmdContext, BlastDbKey<?> blastDbKey) {
		BlastDB blastDB;
		synchronized(blastDbsMap) {
			blastDB = blastDbsMap.get(blastDbKey);
			if(blastDB == null) {
				blastDB = blastDbKey.createBlastDB();
				blastDbsMap.put(blastDbKey, blastDB);
			}
		}
		File blastDbDir = getBlastDbDir(cmdContext, blastDB);
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
	
	private File getBlastDbDir(CommandContext cmdContext, BlastDB blastDB) {
		return blastDB.getBlastDbDir(cmdContext);
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



	
}
