package uk.ac.gla.cvr.gluetools.programs.blast.dbManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;


public class TemporarySingleSeqBlastDB extends BlastDB {
	
	private String uuid;
	private String referenceNTs;
	private long creationTime;
	
	public TemporarySingleSeqBlastDB(String projectName, String uuid, String referenceNTs) {
		super(projectName);
		this.uuid = uuid;
		this.referenceNTs = referenceNTs;
		this.creationTime = System.currentTimeMillis();
	}

	public static class TemporarySingleSeqBlastDbKey extends BlastDbKey<TemporarySingleSeqBlastDB> {

		private String projectName;
		private String uuid;
		private String referenceNTs;
		
		public TemporarySingleSeqBlastDbKey(String projectName, String uuid, String referenceNTs) {
			this.projectName = projectName;
			this.uuid = uuid;
			this.referenceNTs = referenceNTs;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((projectName == null) ? 0 : projectName.hashCode());
			result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
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
			TemporarySingleSeqBlastDbKey other = (TemporarySingleSeqBlastDbKey) obj;
			if (projectName == null) {
				if (other.projectName != null)
					return false;
			} else if (!projectName.equals(other.projectName))
				return false;
			if (uuid == null) {
				if (other.uuid != null)
					return false;
			} else if (!uuid.equals(other.uuid))
				return false;
			return true;
		}

		@Override
		public TemporarySingleSeqBlastDB createBlastDB() {
			return new TemporarySingleSeqBlastDB(projectName, uuid, referenceNTs);
		}
	}

	@Override
	protected File getProjectRelativeBlastDbDir(File projectPath) {
		return new File(projectPath, "temp_"+uuid);
	}

	@Override
	public String getTitle() {
		return "Temporary BLAST DB "+uuid+" in project "+getProjectName();
	}

	@Override
	public long getLastUpdateTime(CommandContext cmdContext) {
		return creationTime;
	}

	@Override
	public InputStream getFastaContentInputStream(CommandContext cmdContext) {
		String fastaString = FastaUtils.seqIdNtsPairToFasta(uuid, referenceNTs);
		return new ByteArrayInputStream(fastaString.getBytes());
	}
	
}