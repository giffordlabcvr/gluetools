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