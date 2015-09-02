package uk.ac.gla.cvr.gluetools.programs.blast.refdb;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Logger;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceShowCreationTimeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceShowCreationTimeCommand.ReferenceShowCreationTimeResult;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceShowSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceShowSequenceCommand.ReferenceShowSequenceResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.OriginalDataResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ShowOriginalDataCommand;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils;
import uk.ac.gla.cvr.gluetools.utils.ProcessUtils.ProcessResult;

public class BlastRefSeqDB {

	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.core");

	private static BlastRefSeqDB instance;
	private LinkedHashMap<RefDBKey, SingleReferenceDB> referenceDBs = 
			new LinkedHashMap<BlastRefSeqDB.RefDBKey, BlastRefSeqDB.SingleReferenceDB>();
	
	public static String 
		MAKEBLASTDB_EXECUTABLE_PROPERTY = "gluetools.core.programs.blast.makeblastdb.executable", 
		BLAST_DB_DIR_PROPERTY = "gluetools.core.programs.blast.db.dir";

	public synchronized static BlastRefSeqDB getInstance() {
		if(instance == null) {
			instance = new BlastRefSeqDB();
		}
		return instance;
	}
	
	private BlastRefSeqDB() {}
	
	public SingleReferenceDB ensureSingleReferenceDB(CommandContext cmdContext, String refName) {
		String projectName = ((ProjectMode) cmdContext.peekCommandMode()).getProject().getName();
		RefDBKey refDbKey = new RefDBKey(projectName, refName);
		SingleReferenceDB refDB;
		synchronized(referenceDBs) {
			refDB = referenceDBs.get(refDbKey);
			if(refDB == null) {
				PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
				String blastDbDir = propertiesConfiguration.getPropertyValue(BLAST_DB_DIR_PROPERTY);
				File projectRefDbDir = new File(blastDbDir, projectName);
				projectRefDbDir.mkdirs();
				File refDbFile = new File(projectRefDbDir, refName);
				refDB = new SingleReferenceDB(refDbFile);
				referenceDBs.put(refDbKey, refDB);
			}
		}
		boolean cleanupRequired = false;
		try {
			refDB.writeLock().lock();
			long refCreationTime = getReferenceCreationTimeResult(cmdContext, refName).getCreationTime();
			File nhrFile = new File(refDB.getFilePath()+".nhr");
			if(nhrFile.exists() && nhrFile.lastModified() > refCreationTime) {
				return refDB;
			}
			cleanupRequired = true;
			String refSeqNtString = referenceSequenceNtString(cmdContext, refName);
			String inputString = ">"+refName+"\n"+refSeqNtString+"\n";
			byte[] inputByteArray = inputString.getBytes();
			PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
			String makeBlastDbExecutable = propertiesConfiguration.getPropertyValue(MAKEBLASTDB_EXECUTABLE_PROPERTY);
			ProcessResult processResult = ProcessUtils.runProcess(inputByteArray, 
					makeBlastDbExecutable, 
					"-in", "-", // sequence data will be piped into the input stream.
					// Note: parse_seqids is unnecessary for single-reference DBs
					// "-parse_seqids", // allows subsequence BLASTN calls to filter reference by sequence IDs 
					"-dbtype", "nucl", // nucleotides
					"-title", refName, // title
					"-out", refDB.getFilePath().getAbsolutePath() // output DB path
					);
			int exitCode = processResult.getExitCode();
			if(exitCode != 0) {
				logger.info(new String(processResult.getOutputBytes()));
				throw new BlastRefSeqDBException(BlastRefSeqDBException.Code.MAKE_BLAST_DB_FAILED, 
						projectName, refName, exitCode, new String(processResult.getErrorBytes()));
			} else {
				cleanupRequired = false;
			}
			return refDB;
		} finally {
			if(cleanupRequired) { cleanup(refDB.getFilePath().getParentFile(), refName); }
			refDB.writeLock().unlock();
		}
	}
	
	private void cleanup(File projectRefDbDir, final String refName) {
		File[] dbFiles = projectRefDbDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(refName);
			}
		});
		for(File dbFile: dbFiles) {
			try {
				Files.delete(dbFile.toPath());
			} catch(Exception e) {
				logger.warning("Unable to clean up reference DB file: "+dbFile.getAbsolutePath());
			}
		}
	}

	public String referenceSequenceNtString(CommandContext cmdContext, String refName) {
		OriginalDataResult refSeqOriginalData = getReferenceSeqOriginalData(cmdContext, refName);
		SequenceFormat refSeqFormat = refSeqOriginalData.getFormat();
		byte[] refSeqBytes = refSeqOriginalData.getBase64Bytes();
		AbstractSequenceObject refSeqObject = refSeqFormat.sequenceObject();
		refSeqObject.fromOriginalData(refSeqBytes);
		return refSeqObject.getNucleotides();
	}

	private OriginalDataResult getReferenceSeqOriginalData(
			CommandContext cmdContext, String refName) {
		// enter the reference command mode to get the reference sourceName and sequence ID.
		ReferenceShowSequenceResult showSequenceResult = getReferenceSequenceResult(cmdContext, refName);
		return getOriginalData(cmdContext, showSequenceResult.getSourceName(), showSequenceResult.getSequenceID());
	}


	private OriginalDataResult getOriginalData(CommandContext cmdContext, String sourceName, String seqId) {
		// enter the sequence command mode to get the sequence original data.
		try (ModeCloser refSeqMode = cmdContext.pushCommandMode("sequence", sourceName, seqId)) {
			return cmdContext.cmdBuilder(ShowOriginalDataCommand.class).execute();
		}
	}

	private ReferenceShowSequenceResult getReferenceSequenceResult(CommandContext cmdContext, String refName) {
		try (ModeCloser refMode = cmdContext.pushCommandMode("reference", refName)) {
			return cmdContext.cmdBuilder(ReferenceShowSequenceCommand.class).execute();
		}
	}

	private ReferenceShowCreationTimeResult getReferenceCreationTimeResult(CommandContext cmdContext, String refName) {
		try (ModeCloser refMode = cmdContext.pushCommandMode("reference", refName)) {
			return cmdContext.cmdBuilder(ReferenceShowCreationTimeCommand.class).execute();
		}
	}

	public class SingleReferenceDB {
		private File filePath;
		private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
		private SingleReferenceDB(File filePath) {
			super();
			this.filePath = filePath;
		}
		private WriteLock writeLock() {
			return readWriteLock.writeLock();
		}
		public ReadLock readLock() {
			return readWriteLock.readLock();
		}

		public File getFilePath() {
			return filePath;
		}
	}
	
	private class RefDBKey {
		private String projectName;
		private String refName;
		public RefDBKey(String projectName, String refName) {
			super();
			this.projectName = projectName;
			this.refName = refName;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((projectName == null) ? 0 : projectName.hashCode());
			result = prime * result
					+ ((refName == null) ? 0 : refName.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RefDBKey other = (RefDBKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (projectName == null) {
				if (other.projectName != null)
					return false;
			} else if (!projectName.equals(other.projectName))
				return false;
			if (refName == null) {
				if (other.refName != null)
					return false;
			} else if (!refName.equals(other.refName))
				return false;
			return true;
		}
		private BlastRefSeqDB getOuterType() {
			return BlastRefSeqDB.this;
		}
		
	}
	
	
}
