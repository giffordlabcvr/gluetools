package uk.ac.gla.cvr.gluetools.programs.blast.dbManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;


public class TemporarySingleSeqBlastDB extends BlastDB<TemporarySingleSeqBlastDB> {
	
	private String uuid;
	private String referenceNTs;
	private String refFastaID;
	private long creationTime;
	
	public TemporarySingleSeqBlastDB(TemporarySingleSeqBlastDbKey key, String uuid, String refFastaID, String referenceNTs) {
		super(key);
		this.uuid = uuid;
		this.refFastaID = refFastaID;
		this.referenceNTs = referenceNTs;
		this.creationTime = System.currentTimeMillis();
	}

	public static class TemporarySingleSeqBlastDbKey extends BlastDbKey<TemporarySingleSeqBlastDB> {

		private String uuid;
		private String refFastaID;
		private String referenceNTs;
		
		public TemporarySingleSeqBlastDbKey(String projectName, String uuid, String refFastaID, String referenceNTs) {
			super(projectName);
			this.uuid = uuid;
			this.refFastaID = refFastaID;
			this.referenceNTs = referenceNTs;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((getProjectName() == null) ? 0 : getProjectName().hashCode());
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
			if (getProjectName() == null) {
				if (other.getProjectName() != null)
					return false;
			} else if (!getProjectName().equals(other.getProjectName()))
				return false;
			if (uuid == null) {
				if (other.uuid != null)
					return false;
			} else if (!uuid.equals(other.uuid))
				return false;
			return true;
		}

		@Override
		protected File getProjectRelativeBlastDbDir(File projectPath) {
			return new File(projectPath, "temp_"+uuid);
		}


		@Override
		public TemporarySingleSeqBlastDB createBlastDB() {
			return new TemporarySingleSeqBlastDB(this, uuid, refFastaID, referenceNTs);
		}
	}

	@Override
	public String getTitle() {
		return "Temporary BLAST DB "+uuid+" in project "+getKey().getProjectName();
	}

	@Override
	public long getLastUpdateTime(CommandContext cmdContext) {
		return creationTime;
	}

	@Override
	public InputStream getFastaContentInputStream(CommandContext cmdContext) {
		String fastaString = FastaUtils.seqIdCompoundsPairToFasta(refFastaID, referenceNTs, LineFeedStyle.LF);
		return new ByteArrayInputStream(fastaString.getBytes());
	}
	
}