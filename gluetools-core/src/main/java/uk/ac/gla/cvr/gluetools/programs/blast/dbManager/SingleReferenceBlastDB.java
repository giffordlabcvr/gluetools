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

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceShowCreationTimeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceShowSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceShowSequenceCommand.ReferenceShowSequenceResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.OriginalDataResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ShowOriginalDataCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;


public class SingleReferenceBlastDB extends BlastDB<SingleReferenceBlastDB> {
	
	private String referenceName;
	
	public SingleReferenceBlastDB(SingleReferenceBlastDbKey key, String referenceName) {
		super(key);
		this.referenceName = referenceName;
	}

	public static class SingleReferenceBlastDbKey extends BlastDbKey<SingleReferenceBlastDB> {

		private String referenceName;

		public SingleReferenceBlastDbKey(String projectName, String referenceName) {
			super(projectName);
			this.referenceName = referenceName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((referenceName == null) ? 0 : referenceName.hashCode());
			result = prime * result
					+ ((getProjectName() == null) ? 0 : getProjectName().hashCode());
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
			SingleReferenceBlastDbKey other = (SingleReferenceBlastDbKey) obj;
			if (referenceName == null) {
				if (other.referenceName != null)
					return false;
			} else if (!referenceName.equals(other.referenceName))
				return false;
			if (getProjectName() == null) {
				if (other.getProjectName() != null)
					return false;
			} else if (!getProjectName().equals(other.getProjectName()))
				return false;
			return true;
		}

		@Override
		protected File getProjectRelativeBlastDbDir(File projectPath) {
			return new File(projectPath, "reference_"+referenceName);
		}


		@Override
		public SingleReferenceBlastDB createBlastDB() {
			return new SingleReferenceBlastDB(this, referenceName);
		}
		
	}

	@Override
	public String getTitle() {
		return "Sequences of reference "+referenceName+" in project "+getKey().getProjectName();
	}

	@Override
	public long getLastUpdateTime(CommandContext cmdContext) {
		try (ModeCloser refMode = cmdContext.pushCommandMode("reference", referenceName)) {
			return cmdContext.cmdBuilder(ReferenceShowCreationTimeCommand.class).execute().getCreationTime();
		}
	}

	@Override
	public InputStream getFastaContentInputStream(CommandContext cmdContext) {
		String referenceSequenceNtString = referenceSequenceNtString(cmdContext, referenceName);
		String fastaString = FastaUtils.seqIdCompoundsPairToFasta(referenceName, referenceSequenceNtString, LineFeedStyle.LF);
		return new ByteArrayInputStream(fastaString.getBytes());
	}

	
	private String referenceSequenceNtString(CommandContext cmdContext, String refName) {
		OriginalDataResult refSeqOriginalData = getReferenceSeqOriginalData(cmdContext, refName);
		AbstractSequenceObject refSeqObject = refSeqOriginalData.getSequenceObject();
		return refSeqObject.getNucleotides(cmdContext);
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

	

	
}