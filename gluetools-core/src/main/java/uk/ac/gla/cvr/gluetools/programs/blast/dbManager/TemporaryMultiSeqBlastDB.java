package uk.ac.gla.cvr.gluetools.programs.blast.dbManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;


public class TemporaryMultiSeqBlastDB extends BlastDB<TemporaryMultiSeqBlastDB> {
	
	private String uuid;
	private Map<String, DNASequence> sequences;
	private long creationTime;
	
	public TemporaryMultiSeqBlastDB(TemporaryMultiSeqBlastDbKey key, String uuid, Map<String, DNASequence> sequences) {
		super(key);
		this.uuid = uuid;
		this.sequences = sequences;
		this.creationTime = System.currentTimeMillis();
	}

	public static class TemporaryMultiSeqBlastDbKey extends BlastDbKey<TemporaryMultiSeqBlastDB> {

		private String uuid;
		private Map<String, DNASequence> sequences;
		
		public TemporaryMultiSeqBlastDbKey(String projectName, String uuid, Map<String, DNASequence> sequences) {
			super(projectName);
			this.uuid = uuid;
			this.sequences = sequences;
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
			TemporaryMultiSeqBlastDbKey other = (TemporaryMultiSeqBlastDbKey) obj;
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
		public TemporaryMultiSeqBlastDB createBlastDB() {
			return new TemporaryMultiSeqBlastDB(this, uuid, sequences);
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
		return new ByteArrayInputStream(FastaUtils.mapToFasta(sequences, LineFeedStyle.LF));
	}
	
}